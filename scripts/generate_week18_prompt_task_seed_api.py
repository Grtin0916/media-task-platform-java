from __future__ import annotations

import csv
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


def read_json(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(str(path))
    return json.loads(path.read_text(encoding="utf-8"))


def read_jsonl(path: Path) -> list[dict]:
    if not path.exists():
        raise FileNotFoundError(str(path))
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line:
            rows.append(json.loads(line))
    return rows


def read_csv(path: Path) -> list[dict]:
    if not path.exists():
        raise FileNotFoundError(str(path))
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def copy_input(src: Path, dst_dir: Path) -> str:
    dst_dir.mkdir(parents=True, exist_ok=True)
    dst = dst_dir / src.name
    shutil.copy2(src, dst)
    return str(dst)


def main() -> int:
    mainbase = Path.home() / "work/grt_work/audio_engineering_repo_skeleton_v1"
    out_dir = Path("artifacts/manifests/week18_prompt_task_seed")
    inputs_dir = out_dir / "inputs"
    out_dir.mkdir(parents=True, exist_ok=True)

    seed_json = mainbase / "reports/week18_seed_from_week17_demo_release_20260703.json"
    tasks_jsonl = mainbase / "reports/week18_prompt_tasks_20260703.jsonl"
    summary_csv = mainbase / "reports/week18_prompt_task_summary_20260703.csv"
    verify_json = mainbase / "reports/week18_prompt_task_verify_20260703.json"
    repair_csv = mainbase / "reports/week18_seed_repair_targets_20260703.csv"

    seed = read_json(seed_json)
    verify = read_json(verify_json)
    tasks = read_jsonl(tasks_jsonl)
    summary_rows = read_csv(summary_csv)
    repair_rows = read_csv(repair_csv)

    case_ids = sorted({t["case_id"] for t in tasks})
    prompt_type_counts = {}
    for t in tasks:
        prompt_type_counts[t["prompt_type"]] = prompt_type_counts.get(t["prompt_type"], 0) + 1

    true_anchor_tasks = [
        t for t in tasks
        if t.get("case_id") == "glass_drop_room_001" and t.get("has_true_mmaudio") is True
    ]

    boundary = verify.get("claim_boundary", {})
    boundary_preserved = all(
        boundary.get(k) is False
        for k in [
            "trueMmaudioBatchSuccess",
            "fullCandidateRankingAvailable",
            "productionSloVerified",
            "k6ThresholdPassVerified",
            "liveGrafanaImportVerified",
        ]
    )

    prompt_task_seed_ready = all([
        verify.get("decision") == "PASS",
        len(tasks) == 12,
        len(case_ids) == 6,
        prompt_type_counts.get("naive") == 6,
        prompt_type_counts.get("dss") == 6,
        len(true_anchor_tasks) == 2,
        len(repair_rows) >= 6,
        boundary_preserved,
    ])

    copied_inputs = {
        "seedJson": copy_input(seed_json, inputs_dir),
        "tasksJsonl": copy_input(tasks_jsonl, inputs_dir),
        "summaryCsv": copy_input(summary_csv, inputs_dir),
        "verifyJson": copy_input(verify_json, inputs_dir),
        "repairCsv": copy_input(repair_csv, inputs_dir),
    }

    report = {
        "contractVersion": "week18-prompt-task-seed-v1",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "promptTaskSeedReady": prompt_task_seed_ready,
        "source": {
            "mainbaseRoot": str(mainbase),
            "seedId": seed.get("seed_id"),
            "mainbaseVerifyDecision": verify.get("decision"),
        },
        "promptTasks": {
            "taskCount": len(tasks),
            "caseCount": len(case_ids),
            "caseIds": case_ids,
            "promptTypeCounts": prompt_type_counts,
            "allCasesHaveNaiveAndDss": verify.get("all_cases_have_naive_and_dss"),
            "trueAnchorTaskCount": len(true_anchor_tasks),
            "repairTargetCount": len(repair_rows),
            "taskPreview": tasks[:2],
        },
        "claimBoundary": {
            **boundary,
            "boundaryPreserved": boundary_preserved,
        },
        "javaApi": {
            "endpoint": "/api/week18/prompt-task-seed",
            "method": "GET",
            "contractPurpose": "Expose Mainbase W18 DSS-vs-naive prompt task queue as a Java platform seed artifact.",
            "randomPortITOnly": True,
            "liveServiceAvailabilityClaimed": False,
        },
        "traceInputs": copied_inputs,
        "nextWeekUse": [
            "Use the JSONL task queue as model-runner input.",
            "Run naive prompt vs DSS prompt ablation.",
            "Promote true anchor as reference only, not batch-success evidence.",
            "Convert failed prompt runs into repair bank targets.",
        ],
    }

    report_path = out_dir / "week18_prompt_task_seed_api_report.json"
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(json.dumps({
        "promptTaskSeedReady": prompt_task_seed_ready,
        "report": str(report_path),
        "taskCount": len(tasks),
        "caseCount": len(case_ids),
        "promptTypeCounts": prompt_type_counts,
        "trueAnchorTaskCount": len(true_anchor_tasks),
        "repairTargetCount": len(repair_rows),
        "boundaryPreserved": boundary_preserved,
    }, ensure_ascii=False, indent=2))

    return 0 if prompt_task_seed_ready else 2


if __name__ == "__main__":
    raise SystemExit(main())