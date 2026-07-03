from __future__ import annotations

import argparse
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


def read_json(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(str(path))
    return json.loads(path.read_text(encoding="utf-8"))


def copy_input(src: Path, dst_dir: Path) -> str:
    dst_dir.mkdir(parents=True, exist_ok=True)
    dst = dst_dir / src.name
    shutil.copy2(src, dst)
    return str(dst)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--mainbase",
        default=str(Path.home() / "work/audio_engineering_repo_skeleton_v1"),
    )
    parser.add_argument(
        "--out-dir",
        default="artifacts/manifests/week17_demo_release_handoff",
    )
    args = parser.parse_args()

    mainbase = Path(args.mainbase).resolve()
    out_dir = Path(args.out_dir)
    inputs_dir = out_dir / "inputs"
    out_dir.mkdir(parents=True, exist_ok=True)

    verify_path = mainbase / "reports/week17_demo_release_verify_20260703.json"
    manifest_path = mainbase / "reports/week17_demo_release_manifest_20260703.json"
    claim_path = mainbase / "reports/week17_demo_claim_boundary_card_20260703.json"
    release_zip = mainbase / "artifacts/demo/week17_true_aware_demo_release_20260703.zip"

    verify = read_json(verify_path)
    manifest = read_json(manifest_path)
    claim = read_json(claim_path)

    checks = verify.get("checks", {})
    boundary_flags = {
        "trueMmaudioBatchSuccess": bool(claim.get("trueMmaudioBatchSuccess")),
        "fullCandidateRankingAvailable": bool(claim.get("fullCandidateRankingAvailable")),
        "productionSloVerified": bool(claim.get("productionSloVerified")),
        "k6ThresholdPassVerified": bool(claim.get("k6ThresholdPassVerified")),
        "liveGrafanaImportVerified": bool(claim.get("liveGrafanaImportVerified")),
    }

    boundary_preserved = all(v is False for v in boundary_flags.values())
    release_handoff_ready = all(
        [
            verify.get("decision") == "PASS",
            checks.get("zip_valid") is True,
            checks.get("zip_contains_index") is True,
            checks.get("zip_contains_wav") is True,
            checks.get("safe_true_mmaudio_record_count", 0) >= 1,
            release_zip.exists(),
            release_zip.stat().st_size > 0,
            boundary_preserved,
        ]
    )

    copied_inputs = {
        "verify": copy_input(verify_path, inputs_dir),
        "manifest": copy_input(manifest_path, inputs_dir),
        "claim": copy_input(claim_path, inputs_dir),
    }

    report = {
        "contractVersion": "week17-demo-release-handoff-v1",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "releaseHandoffReady": release_handoff_ready,
        "source": {
            "mainbaseRoot": str(mainbase),
            "releaseZip": str(release_zip),
            "releaseZipExists": release_zip.exists(),
            "releaseZipSizeBytes": release_zip.stat().st_size if release_zip.exists() else 0,
            "mainbaseVerifyDecision": verify.get("decision"),
            "mainbaseManifestReleaseId": manifest.get("release_id"),
            "mainbaseClaimDecision": claim.get("decision"),
        },
        "audioDemo": {
            "wavCount": manifest.get("wav_count"),
            "trueMmaudioWavCount": manifest.get("true_mmaudio_wav_count"),
            "safeTrueMmaudioRecordCount": claim.get("safeTrueMmaudioRecordCount"),
            "zipContainsIndex": checks.get("zip_contains_index"),
            "zipContainsWav": checks.get("zip_contains_wav"),
            "zipValid": checks.get("zip_valid"),
        },
        "claimBoundary": {
            **boundary_flags,
            "boundaryPreserved": boundary_preserved,
            "allowedClaims": claim.get("allowed_claims", []),
            "blockedClaims": claim.get("blocked_claims", []),
        },
        "javaApi": {
            "endpoint": "/api/week17/demo-release-handoff",
            "method": "GET",
            "contractPurpose": "Expose Mainbase release candidate as a Java platform handoff artifact.",
            "randomPortITOnly": True,
            "liveServiceAvailabilityClaimed": False,
        },
        "traceInputs": copied_inputs,
    }

    report_path = out_dir / "week17_demo_release_handoff_report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(json.dumps({
        "releaseHandoffReady": release_handoff_ready,
        "report": str(report_path),
        "releaseZipSizeBytes": report["source"]["releaseZipSizeBytes"],
        "safeTrueMmaudioRecordCount": report["audioDemo"]["safeTrueMmaudioRecordCount"],
        "boundaryPreserved": boundary_preserved,
    }, ensure_ascii=False, indent=2))

    return 0 if release_handoff_ready else 2


if __name__ == "__main__":
    raise SystemExit(main())