#!/usr/bin/env python3
"""
Build Week13 Java candidate bank demo consumption report.

This script consumes Mainbase's unified candidate bank demo index and turns it
into a Java-platform-side consumption report.

It verifies:
- Mainbase demo index is PASS.
- Candidate count is 10.
- Worker success count is 10.
- Java registry/readiness evidence is PASS.
- Boundary does not overclaim semantic quality, human audition, production K8s,
  cloud object storage, or durable registry.
"""

import argparse
import json
import shutil
from datetime import datetime
from pathlib import Path


def read_json(path, required=True):
    path = Path(path)
    if not path.exists():
        if required:
            raise FileNotFoundError("missing required json: {}".format(path))
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def get_nested(obj, *keys, default=None):
    cur = obj
    for key in keys:
        if not isinstance(cur, dict) or key not in cur:
            return default
        cur = cur[key]
    return cur


def status_of(obj):
    if not isinstance(obj, dict):
        return "MISSING"
    return obj.get("status", "UNKNOWN")


def blockers_of(obj):
    if not isinstance(obj, dict):
        return []
    val = obj.get("blockers", [])
    return val if isinstance(val, list) else []


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--java-root", type=Path, default=Path("."))
    ap.add_argument(
        "--mainbase-index",
        type=Path,
        default=Path("../audio_engineering_repo_skeleton_v1/artifacts/manifests/week13_candidate_bank_demo_index.json"),
    )
    ap.add_argument(
        "--fixture-copy",
        type=Path,
        default=Path("artifacts/fixtures/week13_candidate_bank_demo_index.json"),
    )
    ap.add_argument(
        "--out",
        type=Path,
        default=Path("artifacts/manifests/week13_java_candidate_bank_demo_consumption_report.json"),
    )
    args = ap.parse_args()

    java_root = args.java_root.resolve()
    mainbase_index_path = args.mainbase_index.resolve()

    registry_report_path = java_root / "artifacts/manifests/week13_java_audio_artifact_registry_contract_report.json"
    materialized_readiness_path = java_root / "artifacts/manifests/week13_java_materialized_audio_registry_readiness_report.json"
    api_contract_report_path = java_root / "artifacts/manifests/week13_materialized_readiness_api_contract_report.json"

    demo = read_json(mainbase_index_path)
    registry = read_json(registry_report_path)
    materialized = read_json(materialized_readiness_path)
    api_contract = read_json(api_contract_report_path)

    fixture_copy = java_root / args.fixture_copy
    fixture_copy.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(mainbase_index_path, fixture_copy)

    expected_boundary = [
        "does_not_claim_semantic_audio_quality",
        "does_not_claim_human_audition_pass",
        "does_not_claim_final_mix_readiness",
        "does_not_claim_production_kubernetes_job",
        "does_not_claim_s3_minio_csi_or_cloud_object_storage",
        "does_not_claim_durable_java_registry",
    ]

    boundary = demo.get("boundary", [])
    counts = demo.get("counts", {})
    statuses = demo.get("statuses", {})

    blockers = []
    for name, obj in [
        ("mainbaseDemoIndex", demo),
        ("javaRegistryContract", registry),
        ("javaMaterializedReadiness", materialized),
        ("javaReadinessApiContract", api_contract),
    ]:
        for b in blockers_of(obj):
            blockers.append("{}:{}".format(name, b))

    hard_checks = {
        "mainbaseDemoIndexPass": demo.get("status") == "PASS",
        "javaRegistryContractPass": registry.get("status") == "PASS",
        "javaMaterializedReadinessPass": materialized.get("status") == "PASS",
        "javaReadinessApiContractPass": api_contract.get("status") == "PASS",
        "candidateCountIsTen": counts.get("candidateCount") == 10,
        "placementTableRowsIsTen": counts.get("placementTableRows") == 10,
        "materializedCountIsTen": counts.get("materializedCount") == 10,
        "mountReadableCountIsTen": counts.get("mountReadableCount") == 10,
        "workerReadyCountIsTen": counts.get("workerReadyCount") == 10,
        "workerSuccessCountIsTen": counts.get("workerSuccessCount") == 10,
        "allExpectedBoundaryPresent": all(x in boundary for x in expected_boundary),
        "noBlockers": len(blockers) == 0,
    }

    status = "PASS" if all(hard_checks.values()) else "FAIL"

    report = {
        "schemaVersion": "week13.java_candidate_bank_demo_consumption.v1",
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "status": status,
        "scope": "java-platform-consumption-report-only",
        "sourceMainbaseDemoIndex": str(mainbase_index_path),
        "fixtureCopy": str(fixture_copy.relative_to(java_root)),
        "javaEvidence": {
            "registryContractReport": str(registry_report_path.relative_to(java_root)),
            "materializedReadinessReport": str(materialized_readiness_path.relative_to(java_root)),
            "readinessApiContractReport": str(api_contract_report_path.relative_to(java_root)),
        },
        "consumedCounts": counts,
        "consumedStatuses": statuses,
        "javaStatuses": {
            "javaRegistryContract": status_of(registry),
            "javaMaterializedReadiness": status_of(materialized),
            "javaReadinessApiContract": status_of(api_contract),
        },
        "boundary": boundary,
        "hardChecks": hard_checks,
        "blockers": blockers,
        "platformDecision": (
            "PASS: Java platform can consume the Week13 candidate bank demo index as a local fixture-backed contract."
            if status == "PASS"
            else "FAIL: Java platform consumption is incomplete; inspect hardChecks and blockers."
        ),
        "nextRecommendedStep": (
            "Expose a focused Java endpoint or controller IT for this fixture only after preserving the local-demo boundary."
        ),
    }

    out = java_root / args.out
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    print(json.dumps({
        "out": str(out),
        "status": status,
        "fixtureCopy": str(fixture_copy),
        "failedChecks": [k for k, v in hard_checks.items() if not v],
        "blockers": blockers,
        "consumedCounts": counts,
    }, indent=2, ensure_ascii=False))

    return 0 if status == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())