#!/usr/bin/env python3
import argparse
import json
import re
from datetime import datetime
from pathlib import Path


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--latest-log", required=True)
    ap.add_argument(
        "--report-dir",
        default="artifacts/test-reports/week13_candidate_bank_demo_readiness_api",
    )
    ap.add_argument(
        "--source-report",
        default="artifacts/manifests/week13_java_candidate_bank_demo_consumption_report.json",
    )
    ap.add_argument(
        "--out",
        default="artifacts/manifests/week13_candidate_bank_demo_readiness_api_contract_report.json",
    )
    args = ap.parse_args()

    latest_log = Path(args.latest_log)
    report_dir = Path(args.report_dir)
    source_report = Path(args.source_report)
    out = Path(args.out)

    log_text = latest_log.read_text(encoding="utf-8", errors="ignore") if latest_log.exists() else ""
    source = json.loads(source_report.read_text(encoding="utf-8"))

    report_files = sorted(
        str(p) for p in report_dir.glob("*Week13CandidateBankDemoReadinessControllerIT*")
    )
    xml_files = sorted(report_dir.glob("TEST-*Week13CandidateBankDemoReadinessControllerIT*.xml"))

    tests_run = None
    failures = None
    errors = None
    skipped = None

    for xml in xml_files:
        text = xml.read_text(encoding="utf-8", errors="ignore")
        m = re.search(
            r'tests="(\d+)".*?errors="(\d+)".*?skipped="(\d+)".*?failures="(\d+)"',
            text,
        )
        if m:
            tests_run = int(m.group(1))
            errors = int(m.group(2))
            skipped = int(m.group(3))
            failures = int(m.group(4))
            break

    if tests_run is None:
        m = re.search(
            r"Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)",
            log_text,
        )
        if m:
            tests_run = int(m.group(1))
            failures = int(m.group(2))
            errors = int(m.group(3))
            skipped = int(m.group(4))

    counts = source.get("consumedCounts", {})
    hard_checks = {
        "sourceConsumptionReportPass": source.get("status") == "PASS",
        "apiItRanAtLeastOneTest": isinstance(tests_run, int) and tests_run >= 1,
        "apiItNoFailures": failures == 0,
        "apiItNoErrors": errors == 0,
        "candidateCountIsTen": counts.get("candidateCount") == 10,
        "workerSuccessCountIsTen": counts.get("workerSuccessCount") == 10,
        "noSourceBlockers": source.get("blockers") == [],
        "testEvidencePresent": (len(report_files) > 0) or (isinstance(tests_run, int) and tests_run >= 1),
    }

    status = "PASS" if all(hard_checks.values()) else "FAIL"

    payload = {
        "schemaVersion": "week13.java_candidate_bank_demo_readiness_api_contract.v1",
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "status": status,
        "scope": "java-http-api-contract-report",
        "endpoint": "/api/week13/candidate-bank-demo-readiness",
        "testClass": "Week13CandidateBankDemoReadinessControllerIT",
        "testMode": "SpringBootTest.RANDOM_PORT",
        "sourceConsumptionReport": str(source_report),
        "testLog": str(latest_log),
        "testReportDir": str(report_dir),
        "testReportFiles": report_files,
        "testSummary": {
            "testsRun": tests_run,
            "failures": failures,
            "errors": errors,
            "skipped": skipped,
        },
        "consumedCounts": counts,
        "javaStatuses": source.get("javaStatuses"),
        "boundary": source.get("boundary"),
        "hardChecks": hard_checks,
        "blockers": [] if status == "PASS" else [k for k, v in hard_checks.items() if not v],
        "platformDecision": (
            "PASS: Java exposes the Week13 candidate bank demo readiness over a focused HTTP API verified by RANDOM_PORT IT."
            if status == "PASS"
            else "FAIL: Java HTTP API contract evidence is incomplete; inspect hardChecks and test logs."
        ),
        "nextRecommendedStep": (
            "Let Cloud consume this Java API contract report as a dashboard-ready platform readiness gate."
        ),
    }

    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(json.dumps({
        "out": str(out),
        "status": status,
        "testSummary": payload["testSummary"],
        "failedChecks": payload["blockers"],
        "reportFiles": report_files,
    }, indent=2, ensure_ascii=False))

    return 0 if status == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())