#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
from pathlib import Path
from typing import Any


DEFAULT_JAVA_SNAPSHOT = "artifacts/manifests/week13_java_audio_artifact_registry_snapshot.json"
DEFAULT_OUT = "artifacts/manifests/week13_java_materialized_audio_registry_readiness_report.json"
DEFAULT_CLOUD = os.environ.get("CLOUD", str(Path.home() / "work/ai-job-platform-cloud"))
DEFAULT_CLOUD_MATERIALIZED = "loadtest/reports/week13_materialized_audio_artifact_manifest.json"
DEFAULT_CLOUD_MOUNT = "loadtest/reports/week13_mount_read_contract.json"
DEFAULT_CLOUD_POD_READ = "loadtest/reports/week13_pod_audio_read_simulation_report.json"


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, obj: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def iter_dicts(x: Any):
    if isinstance(x, dict):
        yield x
        for v in x.values():
            yield from iter_dicts(v)
    elif isinstance(x, list):
        for v in x:
            yield from iter_dicts(v)


def get_candidate_id(d: dict[str, Any]) -> str | None:
    for k in ["candidateId", "candidate_id", "id", "audioCandidateId", "audio_candidate_id"]:
        v = d.get(k)
        if isinstance(v, str) and v.strip():
            return v.strip()
    return None


def extract_candidate_records(obj: Any) -> dict[str, dict[str, Any]]:
    out: dict[str, dict[str, Any]] = {}
    for d in iter_dicts(obj):
        cid = get_candidate_id(d)
        if not cid:
            continue
        # 只保留看起来像候选音频记录的对象，避免误抓 summary 层。
        useful = any(k in d for k in [
            "audioUri", "objectKey", "podPath", "localObjectPath",
            "assetTimeMode", "sourceType", "expectedStartSec",
            "audioReadable", "sampleRateHz", "durationSec",
        ])
        if useful:
            out[cid] = dict(d)
    return out


def pick(d: dict[str, Any] | None, keys: list[str], default=None):
    if not d:
        return default
    for k in keys:
        if k in d:
            return d.get(k)
    return default


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--java-snapshot", default=DEFAULT_JAVA_SNAPSHOT)
    ap.add_argument("--cloud-root", default=DEFAULT_CLOUD)
    ap.add_argument("--cloud-materialized", default=DEFAULT_CLOUD_MATERIALIZED)
    ap.add_argument("--cloud-mount", default=DEFAULT_CLOUD_MOUNT)
    ap.add_argument("--cloud-pod-read", default=DEFAULT_CLOUD_POD_READ)
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args()

    java_root = Path.cwd().resolve()
    cloud_root = Path(args.cloud_root).expanduser().resolve()

    java_snapshot_path = java_root / args.java_snapshot
    cloud_materialized_path = cloud_root / args.cloud_materialized
    cloud_mount_path = cloud_root / args.cloud_mount
    cloud_pod_read_path = cloud_root / args.cloud_pod_read
    out_path = java_root / args.out

    java_snapshot = load_json(java_snapshot_path)
    cloud_materialized = load_json(cloud_materialized_path)
    cloud_mount = load_json(cloud_mount_path)
    cloud_pod_read = load_json(cloud_pod_read_path)

    blockers: list[str] = []

    for name, obj in [
        ("java_snapshot", java_snapshot),
        ("cloud_materialized", cloud_materialized),
        ("cloud_mount", cloud_mount),
        ("cloud_pod_read", cloud_pod_read),
    ]:
        if isinstance(obj, dict) and obj.get("status") != "PASS":
            blockers.append(f"{name}_status={obj.get('status')}")

    java_records = extract_candidate_records(java_snapshot)
    mat_records = extract_candidate_records(cloud_materialized)
    mount_records = extract_candidate_records(cloud_mount)
    pod_records = extract_candidate_records(cloud_pod_read)

    java_ids = set(java_records)
    pod_ids = set(pod_records)
    mat_ids = set(mat_records)
    mount_ids = set(mount_records)

    missing_in_java = sorted((pod_ids | mat_ids | mount_ids) - java_ids)
    missing_in_cloud = sorted(java_ids - pod_ids)
    missing_materialized = sorted(java_ids - mat_ids)
    missing_mount = sorted(java_ids - mount_ids)

    if missing_in_java:
        blockers.append(f"missing_in_java={missing_in_java}")
    if missing_in_cloud:
        blockers.append(f"missing_in_cloud_pod_read={missing_in_cloud}")
    if missing_materialized:
        blockers.append(f"missing_cloud_materialized={missing_materialized}")
    if missing_mount:
        blockers.append(f"missing_cloud_mount={missing_mount}")

    enriched = []
    for cid in sorted(java_ids):
        jr = java_records[cid]
        mr = mat_records.get(cid)
        rr = mount_records.get(cid)
        pr = pod_records.get(cid)

        audio_readable = bool(pick(pr, ["audioReadable"], False))
        sha_ok = bool(pick(pr, ["sha256Verified"], False))
        size_ok = bool(pick(pr, ["sizeVerified"], False))
        pod_ok = bool(pick(pr, ["podPathMapped"], False))
        exists_ok = bool(pick(pr, ["localExists"], False))
        materialized = bool(pick(mr, ["materialized"], False))

        ready = materialized and audio_readable and sha_ok and size_ok and pod_ok and exists_ok

        if not ready:
            blockers.append(f"not_ready:{cid}")

        enriched.append({
            "candidateId": cid,
            "audioUri": pick(jr, ["audioUri", "audio_uri"]),
            "sourceType": pick(jr, ["sourceType", "source_type"]),
            "assetTimeMode": pick(jr, ["assetTimeMode", "asset_time_mode"], pick(pr, ["assetTimeMode"])),
            "expectedStartSec": pick(jr, ["expectedStartSec", "expected_start_sec"], pick(pr, ["expectedStartSec"])),
            "placementRequired": pick(jr, ["placementRequired", "placement_required"], pick(pr, ["placementRequired"])),
            "registryStatus": pick(jr, ["status"], "UNKNOWN"),
            "materializedStorageStatus": "READY" if ready else "NOT_READY",
            "objectKey": pick(pr, ["objectKey"], pick(mr, ["objectKey"])),
            "podPath": pick(pr, ["podPath"], pick(rr, ["podPath"])),
            "localObjectPath": pick(pr, ["localObjectPath"], pick(mr, ["localObjectPath"])),
            "sizeBytes": pick(pr, ["sizeBytes"], pick(mr, ["localSizeBytes"])),
            "sha256": pick(pr, ["sha256"], pick(mr, ["localSha256"])),
            "audioReadable": audio_readable,
            "sampleRateHz": pick(pr, ["sampleRateHz"]),
            "channels": pick(pr, ["channels"]),
            "durationSec": pick(pr, ["durationSec"]),
            "sha256Verified": sha_ok,
            "sizeVerified": size_ok,
            "podPathMapped": pod_ok,
            "localExists": exists_ok,
        })

    ready_count = sum(1 for r in enriched if r["materializedStorageStatus"] == "READY")
    candidate_count = len(enriched)

    mode_counts: dict[str, int] = {}
    for r in enriched:
        mode = r.get("assetTimeMode") or "UNKNOWN"
        mode_counts[mode] = mode_counts.get(mode, 0) + 1

    status = "PASS" if candidate_count == 10 and ready_count == 10 and not blockers else "FAIL"

    report = {
        "status": status,
        "scope": "week13_java_consumes_cloud_materialized_audio_registry_v1",
        "generatedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "sourceJavaSnapshot": str(java_snapshot_path),
        "sourceCloudMaterialized": str(cloud_materialized_path),
        "sourceCloudMountContract": str(cloud_mount_path),
        "sourceCloudPodRead": str(cloud_pod_read_path),
        "candidateCount": candidate_count,
        "readyCount": ready_count,
        "missingInJavaCount": len(missing_in_java),
        "missingInCloudCount": len(missing_in_cloud),
        "missingMaterializedCount": len(missing_materialized),
        "missingMountCount": len(missing_mount),
        "assetTimeModeCounts": mode_counts,
        "blockers": blockers,
        "boundary": [
            "Java consumes Cloud materialized readiness evidence",
            "does not declare durable registry persistence",
            "does not declare production object storage",
            "does not declare worker orchestration yet",
            "next step may expose these fields through focused Spring Boot API contract",
        ],
        "records": enriched,
    }

    write_json(out_path, report)

    print(json.dumps({
        "report": str(out_path),
        "status": report["status"],
        "candidateCount": candidate_count,
        "readyCount": ready_count,
        "assetTimeModeCounts": mode_counts,
        "missingInJavaCount": report["missingInJavaCount"],
        "missingInCloudCount": report["missingInCloudCount"],
        "blockers": blockers,
    }, ensure_ascii=False, indent=2))

    return 0 if status == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
