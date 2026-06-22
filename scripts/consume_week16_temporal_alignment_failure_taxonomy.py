#!/usr/bin/env python3
import json
import subprocess
import sys
from pathlib import Path
from datetime import datetime, timezone

def sh(cmd, cwd=None):
    return subprocess.check_output(cmd, text=True, cwd=cwd).strip()

def load_json(path):
    if not path.exists():
        raise SystemExit(f"MISSING_INPUT: {path}")
    return json.loads(path.read_text(encoding="utf-8"))

def main():
    if len(sys.argv) != 2:
        raise SystemExit("usage: consume_week16_temporal_alignment_failure_taxonomy.py <mainbase_taxonomy_json>")

    upstream = Path(sys.argv[1]).expanduser().resolve()
    data = load_json(upstream)
    rows = data.get("taxonomyRows", [])
    summary = data.get("summary", {})

    out_report = Path("artifacts/manifests/week16_java_temporal_alignment_failure_taxonomy_consumer_report.json")
    out_payload = Path("artifacts/manifests/week16_java_temporal_alignment_failure_taxonomy_payload.json")

    p1_regression = [
        {
            "candidateId": r.get("candidateId"),
            "bucket": r.get("w16FailureBucket"),
            "severity": r.get("severity"),
            "originalStatus": r.get("originalStatus"),
            "originalAbsOnsetDeltaSec": r.get("originalAbsOnsetDeltaSec"),
            "remediatedStatus": r.get("remediatedStatus"),
            "remediatedAbsOnsetDeltaSec": r.get("remediatedAbsOnsetDeltaSec"),
            "hasWaveformEvidence": r.get("hasWaveformEvidence"),
            "recommendedNextAction": r.get("recommendedNextAction"),
        }
        for r in rows
        if r.get("w16FailureBucket") == "timing_drift_actionable_remediated"
    ]

    threshold_fixtures = [
        {
            "candidateId": r.get("candidateId"),
            "bucket": r.get("w16FailureBucket"),
            "severity": r.get("severity"),
            "originalStatus": r.get("originalStatus"),
            "originalAbsOnsetDeltaSec": r.get("originalAbsOnsetDeltaSec"),
            "recommendedNextAction": r.get("recommendedNextAction"),
        }
        for r in rows
        if r.get("w16FailureBucket") == "warn_near_miss_threshold_margin"
    ]

    pass_controls = [
        {
            "candidateId": r.get("candidateId"),
            "originalAbsOnsetDeltaSec": r.get("originalAbsOnsetDeltaSec"),
            "remediatedAbsOnsetDeltaSec": r.get("remediatedAbsOnsetDeltaSec"),
            "severity": r.get("severity"),
        }
        for r in rows
        if r.get("w16FailureBucket") == "pass_low_risk_with_numeric_margin"
    ]

    payload = {
        "schemaVersion": "week16.java.temporal_alignment.failure_taxonomy.payload.v1",
        "sourceMode": "mainbase_week16_failure_taxonomy_seed",
        "apiCandidate": "/api/week16/temporal-alignment/failure-taxonomy",
        "candidateTotal": summary.get("candidateTotal"),
        "bucketCounts": summary.get("bucketCounts"),
        "regressionFixtures": p1_regression,
        "thresholdFixtures": threshold_fixtures,
        "passControlFixtures": pass_controls,
        "blockedClaims": [
            "No semantic audio quality pass claim.",
            "No human-review pass claim.",
            "No final mix readiness claim.",
            "No production SLO claim."
        ],
    }

    errors = []
    if data.get("decision") != "PASS_WEEK16_TEMPORAL_ALIGNMENT_FAILURE_TAXONOMY_SEED_V3_SOURCE_CLEAN":
        errors.append(f"unexpected upstream decision: {data.get('decision')}")
    if summary.get("candidateTotal") != 10:
        errors.append(f"candidateTotal expected 10 got {summary.get('candidateTotal')}")
    if len(p1_regression) != 2:
        errors.append(f"P1 regression fixture count expected 2 got {len(p1_regression)}")
    if [x.get("candidateId") for x in p1_regression] != ["procedural_v0_0004", "procedural_v0_0010"]:
        errors.append(f"unexpected P1 regression ids: {[x.get('candidateId') for x in p1_regression]}")
    if len(threshold_fixtures) != 1 or threshold_fixtures[0].get("candidateId") != "procedural_v0_0007":
        errors.append(f"unexpected threshold fixtures: {threshold_fixtures}")
    if len(pass_controls) != 7:
        errors.append(f"pass controls expected 7 got {len(pass_controls)}")
    if any(x.get("hasWaveformEvidence") is not True for x in p1_regression):
        errors.append("P1 regression fixtures must have waveform evidence")
    if summary.get("statusKnownCount") != 10 or summary.get("deltaKnownCount") != 10:
        errors.append("upstream status/delta coverage is incomplete")

    decision = (
        "PASS_WEEK16_JAVA_TEMPORAL_ALIGNMENT_FAILURE_TAXONOMY_CONSUMER"
        if not errors
        else "FAIL_WEEK16_JAVA_TEMPORAL_ALIGNMENT_FAILURE_TAXONOMY_CONSUMER"
    )

    report = {
        "schemaVersion": "week16.java.temporal_alignment.failure_taxonomy.consumer_report.v1",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "decision": decision,
        "decisionErrors": errors,
        "source": {
            "mainbaseTaxonomyPath": str(upstream),
            "mainbaseHead": sh(["git", "rev-parse", "--short", "HEAD"], cwd=upstream.parents[2]),
            "mainbaseOriginMain": sh(["git", "rev-parse", "--short", "origin/main"], cwd=upstream.parents[2]),
            "upstreamDecision": data.get("decision"),
            "upstreamSchemaVersion": data.get("schemaVersion"),
        },
        "javaGit": {
            "head": sh(["git", "rev-parse", "--short", "HEAD"]),
            "originMain": sh(["git", "rev-parse", "--short", "origin/main"]),
            "aheadBehind": sh(["git", "rev-list", "--left-right", "--count", "HEAD...origin/main"]),
        },
        "apiCandidate": payload["apiCandidate"],
        "payloadPath": str(out_payload),
        "summary": {
            "candidateTotal": payload["candidateTotal"],
            "bucketCounts": payload["bucketCounts"],
            "p1RegressionFixtureIds": [x.get("candidateId") for x in p1_regression],
            "thresholdFixtureIds": [x.get("candidateId") for x in threshold_fixtures],
            "passControlCount": len(pass_controls),
            "blockedClaims": payload["blockedClaims"],
        },
        "boundary": [
            "Java artifact consumer evidence only.",
            "Does not claim live Java endpoint availability for W16 yet.",
            "Does not claim production SLO.",
            "Does not claim semantic audio quality pass.",
        ],
    }

    out_payload.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    out_report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(json.dumps({
        "decision": decision,
        "decisionErrors": errors,
        "report": str(out_report),
        "payload": str(out_payload),
        "summary": report["summary"],
    }, ensure_ascii=False, indent=2))

    if errors:
        raise SystemExit(2)

if __name__ == "__main__":
    main()
