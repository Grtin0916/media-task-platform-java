#!/usr/bin/env python3
import json
from datetime import datetime, timezone
from pathlib import Path

SOURCE = Path("artifacts/manifests/week15_mainbase_explicit_risk_contract.json")
OUT = Path("artifacts/manifests/week15_java_explicit_risk_contract_consumer_report.json")

data = json.loads(SOURCE.read_text(encoding="utf-8"))
summary = data.get("summary") or {}

actionable = summary.get("actionableRiskCandidateIds") or []
alert_eligible = summary.get("alertEligibleCandidateIds") or []
non_actionable = summary.get("nonActionableCandidateIds") or []

failures = []
if data.get("decision") != "PASS":
    failures.append("SOURCE_CONTRACT_NOT_PASS")
if actionable != ["procedural_v0_0004", "procedural_v0_0010"]:
    failures.append("ACTIONABLE_SET_UNEXPECTED")
if actionable != alert_eligible:
    failures.append("ACTIONABLE_ALERT_ELIGIBLE_MISMATCH")
if len(non_actionable) != 8:
    failures.append("NON_ACTIONABLE_COUNT_UNEXPECTED")

report = {
    "schemaVersion": "week15.java.explicit_risk_contract.consumer_report.v1",
    "generatedAt": datetime.now(timezone.utc).isoformat(),
    "decision": "PASS" if not failures else "FAIL",
    "failures": failures,
    "sourceContract": str(SOURCE),
    "apiEndpoint": "/api/week15/temporal-alignment/explicit-risk-contract",
    "summary": {
        "candidateTotal": summary.get("candidateTotal"),
        "actionableRiskCandidateIds": actionable,
        "alertEligibleCandidateIds": alert_eligible,
        "nonActionableCandidateIds": non_actionable,
        "blockedClaims": summary.get("blockedClaims"),
    },
    "nextAction": (
        "Cloud can switch from inferred taxonomy to Java explicit risk contract API."
        if not failures
        else "Fix Java explicit risk contract consumer before Cloud integration."
    )
}

OUT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print(json.dumps(report, indent=2, ensure_ascii=False))

if failures:
    raise SystemExit("JAVA_EXPLICIT_RISK_CONTRACT_CONSUMER_REPORT_FAIL")
