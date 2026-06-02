#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json
from datetime import datetime, timezone

ROOT = Path.cwd()
MIGRATION_DIRS = [
    ROOT / "src/main/resources/db/migration",
    ROOT / "src/test/resources/db/migration",
]

BANNED = {
    "timestamptz": "Use 'timestamp with time zone' for PostgreSQL/H2-compatible Flyway migration SQL.",
}

CHECKED_SUFFIXES = {".sql"}

hits = []
checked = []

for d in MIGRATION_DIRS:
    if not d.exists():
        continue
    for path in sorted(d.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in CHECKED_SUFFIXES:
            continue
        rel = str(path.relative_to(ROOT))
        checked.append(rel)
        text = path.read_text(encoding="utf-8", errors="replace")
        lower = text.lower()
        for token, message in BANNED.items():
            if token in lower:
                hits.append({
                    "file": rel,
                    "token": token,
                    "message": message,
                })

report = {
    "schemaVersion": "week12.sql-dialect-compat.v1",
    "generatedAt": datetime.now(timezone.utc).isoformat(),
    "status": "PASS" if not hits else "FAIL",
    "checkedFiles": checked,
    "bannedTokenHits": hits,
    "purpose": "Prevent PostgreSQL-only timestamptz abbreviation from breaking H2-backed Flyway runtime tests.",
    "doesNotClaim": [
        "full PostgreSQL migration verification",
        "Java runtime HTTP success",
        "Spring application context startup success",
    ],
}

out = ROOT / "artifacts/logs/week12_sql_dialect_compat_report.json"
out.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

print("status=", report["status"])
print("checked_files=", len(checked))
print("banned_hits=", len(hits))
print("report=", out)

if hits:
    raise SystemExit(2)
