#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path.cwd()
LOG_PATH = ROOT / "artifacts/logs/week13_audio_artifact_registry_it.log"
REPORT_PATH = ROOT / "artifacts/manifests/week13_java_audio_artifact_registry_contract_report.json"
FIXTURE_PATH = ROOT / "artifacts/fixtures/week13_mainbase_mix_preview_manifest.json"
SUREFIRE_DIR = ROOT / "target/surefire-reports"


def git_head() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=ROOT,
            text=True,
        ).strip()
    except Exception:
        return "UNKNOWN"


def parse_surefire() -> dict:
    xml_files = sorted(SUREFIRE_DIR.glob("TEST-*.xml")) if SUREFIRE_DIR.exists() else []
    target_files = [
        p for p in xml_files
        if "Week13AudioArtifactRegistryTest" in p.name or "Week13AudioArtifactRegistryIT" in p.name
    ]

    parsed = []
    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0

    for p in target_files:
        root = ET.parse(p).getroot()
        tests = int(root.attrib.get("tests", "0"))
        failures = int(root.attrib.get("failures", "0"))
        errors = int(root.attrib.get("errors", "0"))
        skipped = int(root.attrib.get("skipped", "0"))

        total_tests += tests
        total_failures += failures
        total_errors += errors
        total_skipped += skipped

        parsed.append({
            "file": str(p.relative_to(ROOT)),
            "tests": tests,
            "failures": failures,
            "errors": errors,
            "skipped": skipped,
        })

    return {
        "surefireDirExists": SUREFIRE_DIR.exists(),
        "targetXmlCount": len(target_files),
        "reports": parsed,
        "tests": total_tests,
        "failures": total_failures,
        "errors": total_errors,
        "skipped": total_skipped,
        "passed": (
            len(target_files) > 0
            and total_tests > 0
            and total_failures == 0
            and total_errors == 0
        ),
    }


def main() -> int:
    log_text = LOG_PATH.read_text(encoding="utf-8", errors="replace") if LOG_PATH.exists() else ""
    fixture = json.loads(FIXTURE_PATH.read_text(encoding="utf-8")) if FIXTURE_PATH.exists() else {}

    surefire = parse_surefire()

    # fallback：只有 Surefire XML 缺失时才退回 Maven log 文本判定。
    log_passed = "BUILD SUCCESS" in log_text
    test_passed = surefire["passed"] or (
        surefire["targetXmlCount"] == 0 and log_passed
    )

    status = "PASS" if test_passed else "FAIL"

    blockers = []
    if not test_passed:
        if surefire["targetXmlCount"] == 0:
            blockers.append("WEEK13_AUDIO_ARTIFACT_REGISTRY_TEST_SUREFIRE_XML_MISSING")
        if surefire["failures"] > 0:
            blockers.append(f"WEEK13_AUDIO_ARTIFACT_REGISTRY_TEST_FAILURES_{surefire['failures']}")
        if surefire["errors"] > 0:
            blockers.append(f"WEEK13_AUDIO_ARTIFACT_REGISTRY_TEST_ERRORS_{surefire['errors']}")
        if "COMPILATION ERROR" in log_text:
            blockers.append("MAVEN_COMPILATION_ERROR")
        if "BUILD FAILURE" in log_text:
            blockers.append("MAVEN_BUILD_FAILURE")
        if not blockers:
            blockers.append("WEEK13_AUDIO_ARTIFACT_REGISTRY_TEST_NOT_PASS")

    report = {
        "status": status,
        "scope": "java_exposes_week13_audio_artifact_registry_contract_v0",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "sourceRepo": {
            "path": str(ROOT),
            "head": git_head(),
        },
        "mainbaseEvidence": {
            "dryRunStatus": fixture.get("status"),
            "candidateCount": fixture.get("candidateCount"),
            "assetTimeModeCounts": fixture.get("assetTimeModeCounts"),
            "fixedPlacementMisplacedCount": fixture.get("fixedPlacementMisplacedCount"),
            "naiveZeroWouldMisplaceCount": fixture.get("naiveZeroWouldMisplaceCount"),
        },
        "endpoints": [
            "GET /api/week13/audio-artifacts",
            "GET /api/week13/audio-artifacts/{candidateId}",
        ],
        "contractFields": [
            "candidateId",
            "audioUri",
            "sourceType",
            "assetTimeMode",
            "expectedStartSec",
            "expectedEndSec",
            "globalStartSec",
            "globalEndSec",
            "placementOffsetSec",
            "placementRequired",
            "status",
        ],
        "test": {
            "name": "Week13AudioArtifactRegistryTest",
            "log": "artifacts/logs/week13_audio_artifact_registry_it.log",
            "surefire": surefire,
            "passed": test_passed,
        },
        "boundaryStatement": (
            "Java exposes Mainbase Week13 placement/dry-run candidate artifacts as an in-memory API contract. "
            "This does not implement durable object storage, database-backed registry, final mixer, semantic quality validation, "
            "human audition, or production readiness."
        ),
        "blockers": blockers,
    }

    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if status == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
