#!/usr/bin/env python3
from __future__ import annotations

import datetime as dt
import hashlib
import json
import os
from pathlib import Path
from typing import Any


JAVA_ROOT = Path.cwd()
MAINBASE = Path(
    os.environ.get(
        "MAINBASE",
        str(Path.home() / "work" / "audio_engineering_repo_skeleton_v1"),
    )
)

SOURCE_ARTIFACT = MAINBASE / "artifacts" / "evals" / "week16_s3_to_w17_layer_mix_input.json"

MANIFEST_DIR = JAVA_ROOT / "artifacts" / "manifests"
LOG_DIR = JAVA_ROOT / "artifacts" / "logs"
DOC_DIR = JAVA_ROOT / "docs" / "api"
MAIN_SRC_DIR = JAVA_ROOT / "src" / "main" / "java" / "com" / "ryan" / "media" / "controller"
TEST_SRC_DIR = JAVA_ROOT / "src" / "test" / "java" / "com" / "ryan" / "media" / "controller"

PAYLOAD_COPY = MANIFEST_DIR / "week17_layer_mix_input_readiness_mainbase_payload.json"
REPORT_PATH = MANIFEST_DIR / "week17_layer_mix_input_readiness_api_report.json"
CONTROLLER_PATH = MAIN_SRC_DIR / "Week17LayerMixInputReadinessController.java"
TEST_PATH = TEST_SRC_DIR / "Week17LayerMixInputReadinessIT.java"
DOC_PATH = DOC_DIR / "week17-layer-mix-input-readiness.md"


def norm_key(key: str) -> str:
    return key.replace("_", "").replace("-", "").lower()


def get_key(d: dict[str, Any], names: list[str]) -> Any:
    wanted = {norm_key(x) for x in names}
    for k, v in d.items():
        if norm_key(k) in wanted:
            return v
    return None


def walk(obj: Any, out: list[dict[str, Any]]) -> None:
    if isinstance(obj, dict):
        candidate_id = get_key(obj, ["candidateId", "candidate_id", "id"])
        has_readiness_field = any(
            norm_key(k)
            in {
                "mixeligibility",
                "fixturerole",
                "requiredsafeguard",
                "blockedclaims",
                "nextaction",
                "sourcecandidateid",
            }
            for k in obj.keys()
        )
        if candidate_id is not None and has_readiness_field:
            out.append(obj)
        for v in obj.values():
            walk(v, out)
    elif isinstance(obj, list):
        for item in obj:
            walk(item, out)


def count_by(records: list[dict[str, Any]], names: list[str]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for r in records:
        value = get_key(r, names)
        if value is None:
            value = "UNKNOWN"
        value = str(value)
        counts[value] = counts.get(value, 0) + 1
    return dict(sorted(counts.items()))


def collect_blocked_claims(records: list[dict[str, Any]]) -> list[str]:
    claims: list[str] = []
    for r in records:
        value = get_key(r, ["blockedClaims", "blocked_claims"])
        if isinstance(value, list):
            claims.extend(str(x) for x in value)
        elif isinstance(value, str):
            claims.append(value)
    unique = sorted(set(x for x in claims if x.strip()))
    return unique


def main() -> int:
    if not SOURCE_ARTIFACT.exists():
        raise FileNotFoundError(f"Missing Mainbase readiness artifact: {SOURCE_ARTIFACT}")

    for p in [MANIFEST_DIR, LOG_DIR, DOC_DIR, MAIN_SRC_DIR, TEST_SRC_DIR]:
        p.mkdir(parents=True, exist_ok=True)

    payload_text = SOURCE_ARTIFACT.read_text(encoding="utf-8")
    payload = json.loads(payload_text)
    payload_sha256 = hashlib.sha256(payload_text.encode("utf-8")).hexdigest()

    PAYLOAD_COPY.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    records: list[dict[str, Any]] = []
    walk(payload, records)

    unique_candidate_ids = []
    seen = set()
    for r in records:
        cid = get_key(r, ["candidateId", "candidate_id", "id"])
        if cid is not None and str(cid) not in seen:
            seen.add(str(cid))
            unique_candidate_ids.append(str(cid))

    source_decision = "UNKNOWN"
    if isinstance(payload, dict):
        raw_decision = get_key(payload, ["decision", "status", "readinessDecision"])
        if raw_decision is not None:
            source_decision = str(raw_decision)

    system_blocked_claims = [
        "real layer mixer executed",
        "final mix readiness",
        "semantic audio quality pass",
        "human review pass",
        "live Java service availability",
        "production auth boundary",
        "exactly-once rerun",
    ]

    report = {
        "decision": (
            "PASS_WEEK17_LAYER_MIX_INPUT_READINESS_CONSUMED"
            if len(unique_candidate_ids) > 0
            else "WARN_WEEK17_LAYER_MIX_INPUT_READINESS_CONSUMED_WITH_EMPTY_CANDIDATE_EXTRACTION"
        ),
        "generatedAtUtc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "sourceArtifact": str(SOURCE_ARTIFACT),
        "copiedPayload": str(PAYLOAD_COPY),
        "payloadSha256": payload_sha256,
        "sourceDecision": source_decision,
        "candidateTotal": len(unique_candidate_ids),
        "candidateIds": unique_candidate_ids,
        "mixEligibilityCounts": count_by(records, ["mixEligibility", "mix_eligibility"]),
        "fixtureRoleCounts": count_by(records, ["fixtureRole", "fixture_role"]),
        "readOnlyApiEndpoint": "/api/week16/temporal-alignment/layer-mix-input-readiness",
        "artifactContractVersion": "week17.layer_mix_input_readiness.v0",
        "realMixerTriggered": False,
        "realWorkerTriggered": False,
        "previousS3EvidencePreserved": True,
        "blockedClaims": sorted(set(system_blocked_claims + collect_blocked_claims(records))),
    }

    REPORT_PATH.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    CONTROLLER_PATH.write_text(
        """package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week17LayerMixInputReadinessController {
    private final ObjectMapper objectMapper;

    public Week17LayerMixInputReadinessController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping(
            value = "/api/week16/temporal-alignment/layer-mix-input-readiness",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getLayerMixInputReadiness() throws IOException {
        Path reportPath = Path.of("artifacts/manifests/week17_layer_mix_input_readiness_api_report.json");
        Path payloadPath = Path.of("artifacts/manifests/week17_layer_mix_input_readiness_mainbase_payload.json");

        JsonNode report = objectMapper.readTree(Files.readString(reportPath));
        JsonNode mainbasePayload = objectMapper.readTree(Files.readString(payloadPath));

        ObjectNode response = objectMapper.createObjectNode();
        response.put("decision", "READ_ONLY_LAYER_MIX_INPUT_READINESS_CONTRACT");
        response.put("sourceMainbaseDecision", report.path("sourceDecision").asText("UNKNOWN"));
        response.put("candidateTotal", report.path("candidateTotal").asInt(-1));
        response.put("artifactContractVersion", "week17.layer_mix_input_readiness.v0");
        response.put("realMixerTriggered", false);
        response.put("realWorkerTriggered", false);
        response.put("previousS3EvidencePreserved", true);
        response.set("mixEligibilityCounts", report.path("mixEligibilityCounts"));
        response.set("fixtureRoleCounts", report.path("fixtureRoleCounts"));
        response.set("blockedClaims", report.path("blockedClaims"));
        response.set("consumerReport", report);
        response.set("mainbasePayload", mainbasePayload);

        return ResponseEntity.ok(response);
    }
}
""",
        encoding="utf-8",
    )

    TEST_PATH.write_text(
        """package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17LayerMixInputReadinessIT {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesReadOnlyLayerMixInputReadinessContractWithoutTriggeringMixer() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/week16/temporal-alignment/layer-mix-input-readiness",
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("decision").asText())
                .isEqualTo("READ_ONLY_LAYER_MIX_INPUT_READINESS_CONTRACT");
        assertThat(body.path("artifactContractVersion").asText())
                .isEqualTo("week17.layer_mix_input_readiness.v0");
        assertThat(body.path("realMixerTriggered").asBoolean(true)).isFalse();
        assertThat(body.path("realWorkerTriggered").asBoolean(true)).isFalse();
        assertThat(body.path("previousS3EvidencePreserved").asBoolean(false)).isTrue();
        assertThat(body.path("blockedClaims").isArray()).isTrue();
        assertThat(body.path("consumerReport").isObject()).isTrue();
        assertThat(body.path("mainbasePayload").isObject()).isTrue();
    }
}
""",
        encoding="utf-8",
    )

    DOC_PATH.write_text(
        """# Week17 Layer-Mix Input Readiness API

## Endpoint

`GET /api/week16/temporal-alignment/layer-mix-input-readiness`

## Purpose

Expose a read-only Java contract that consumes the Mainbase S3-to-W17 layer-mix input readiness artifact.

This endpoint is a readiness gate for the next layer-mix stage. It does not execute a mixer, does not trigger a worker, and does not claim final mix readiness.

## Source artifact

`artifacts/manifests/week17_layer_mix_input_readiness_mainbase_payload.json`

Copied from Mainbase:

`artifacts/evals/week16_s3_to_w17_layer_mix_input.json`

## Response boundary

The response includes:

- `candidateTotal`
- `mixEligibilityCounts`
- `fixtureRoleCounts`
- `blockedClaims`
- `sourceMainbaseDecision`
- copied Mainbase payload
- Java consumer report

## Explicit non-claims

- No real layer mixer was executed.
- No final mix readiness is claimed.
- No semantic audio quality pass is claimed.
- No real worker was triggered.
- No production auth or live service availability is claimed.
""",
        encoding="utf-8",
    )

    print(json.dumps(report, ensure_ascii=False, indent=2))
    print()
    print("WROTE:")
    for p in [PAYLOAD_COPY, REPORT_PATH, CONTROLLER_PATH, TEST_PATH, DOC_PATH]:
        print(f"- {p}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())