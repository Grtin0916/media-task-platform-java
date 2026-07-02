from __future__ import annotations

import json
import shutil
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


JAVA_ROOT = Path(".").resolve()
MAINBASE = Path.home() / "work/audio_engineering_repo_skeleton_v1"

SRC_BRIDGE = MAINBASE / "reports/week17_true_aware_platform_bridge_payload_20260702.json"
SRC_CLAIM_GUARD = MAINBASE / "reports/week17_true_aware_claim_guard_20260702.json"
SRC_RAW_PAYLOAD = MAINBASE / "reports/week17_true_aware_result_card_payload_20260702.json"

MANIFEST_DIR = JAVA_ROOT / "artifacts/manifests/week17_true_aware_result_card"
DOC_PATH = JAVA_ROOT / "docs/api/week17-true-aware-result-card.md"
CONTROLLER_PATH = JAVA_ROOT / "src/main/java/com/ryan/media/controller/Week17TrueAwareResultCardController.java"
IT_PATH = JAVA_ROOT / "src/test/java/com/ryan/media/controller/Week17TrueAwareResultCardControllerIT.java"
API_REPORT_PATH = MANIFEST_DIR / "week17_true_aware_result_card_api_report.json"


def load_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(path)
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise TypeError(f"{path} must contain a JSON object")
    return data


def copy_required_inputs() -> dict[str, str]:
    MANIFEST_DIR.mkdir(parents=True, exist_ok=True)

    dst_bridge = MANIFEST_DIR / SRC_BRIDGE.name
    dst_guard = MANIFEST_DIR / SRC_CLAIM_GUARD.name
    dst_raw = MANIFEST_DIR / SRC_RAW_PAYLOAD.name

    shutil.copy2(SRC_BRIDGE, dst_bridge)
    shutil.copy2(SRC_CLAIM_GUARD, dst_guard)
    shutil.copy2(SRC_RAW_PAYLOAD, dst_raw)

    return {
        "bridge": str(dst_bridge),
        "claim_guard": str(dst_guard),
        "raw_payload": str(dst_raw),
    }


def validate_bridge(bridge: dict[str, Any]) -> None:
    strict = bridge.get("strict_boundary", {})
    card = bridge.get("platform_result_card", {})

    required = {
        "true_mmaudio_single_success": True,
        "true_mmaudio_audio_artifact_count": 1,
        "true_mmaudio_case_count": 1,
        "true_mmaudio_batch_success": False,
        "full_candidate_ranking_available": False,
        "production_slo_verified": False,
        "k6_threshold_pass_verified": False,
        "hf_cache_or_model_weight_included": False,
    }

    for key, expected in required.items():
        actual = strict.get(key)
        if actual != expected:
            raise ValueError(f"Boundary mismatch: {key} expected={expected!r} actual={actual!r}")

    if card.get("status") != "consumer_ready":
        raise ValueError(f"platform_result_card.status must be consumer_ready, got {card.get('status')!r}")

    if card.get("safe_true_mmaudio_record_count") != 1:
        raise ValueError("safe_true_mmaudio_record_count must stay 1")


def write_controller() -> None:
    CONTROLLER_PATH.parent.mkdir(parents=True, exist_ok=True)

    CONTROLLER_PATH.write_text(
        """package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/week17/true-aware/result-card")
public class Week17TrueAwareResultCardController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Path BRIDGE_PATH = Path.of(
            "artifacts/manifests/week17_true_aware_result_card/week17_true_aware_platform_bridge_payload_20260702.json");

    private static final Path CLAIM_GUARD_PATH = Path.of(
            "artifacts/manifests/week17_true_aware_result_card/week17_true_aware_claim_guard_20260702.json");

    private static JsonNode readJson(Path path) {
        if (!Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing artifact: " + path);
        }
        try {
            return MAPPER.readTree(path.toFile());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid artifact JSON: " + path, ex);
        }
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("artifactBacked", true);
        body.put("bridgeExists", Files.exists(BRIDGE_PATH));
        body.put("claimGuardExists", Files.exists(CLAIM_GUARD_PATH));
        body.put("claimLevel", readJson(CLAIM_GUARD_PATH).path("claim_level").asText());
        return body;
    }

    @GetMapping("/bridge")
    public JsonNode bridge() {
        return readJson(BRIDGE_PATH);
    }

    @GetMapping("/claim-guard")
    public JsonNode claimGuard() {
        return readJson(CLAIM_GUARD_PATH);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        JsonNode bridge = readJson(BRIDGE_PATH);
        JsonNode strict = bridge.path("strict_boundary");
        JsonNode card = bridge.path("platform_result_card");
        JsonNode guard = bridge.path("claim_guard");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("schemaVersion", bridge.path("schema_version").asText());
        body.put("status", card.path("status").asText());
        body.put("claimLevel", guard.path("claim_level").asText());
        body.put("primaryCaseId", card.path("primary_case_id").asText());
        body.put("primaryModel", card.path("primary_model").asText());
        body.put("primaryAudioArtifact", card.path("primary_audio_artifact").asText());
        body.put("primaryAudioSha256", card.path("primary_audio_sha256").asText());
        body.put("safeTrueMmaudioRecordCount", card.path("safe_true_mmaudio_record_count").asInt());
        body.put("rawCandidateRecordCount", card.path("raw_candidate_record_count").asInt());
        body.put("rawWinnerRecordCount", card.path("raw_winner_record_count").asInt());
        body.put("trueMmaudioSingleSuccess", strict.path("true_mmaudio_single_success").asBoolean());
        body.put("trueMmaudioBatchSuccess", strict.path("true_mmaudio_batch_success").asBoolean());
        body.put("fullCandidateRankingAvailable", strict.path("full_candidate_ranking_available").asBoolean());
        body.put("productionSloVerified", strict.path("production_slo_verified").asBoolean());
        body.put("k6ThresholdPassVerified", strict.path("k6_threshold_pass_verified").asBoolean());
        return body;
    }

    @GetMapping("/artifacts")
    public Map<String, Object> artifacts() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bridgePath", BRIDGE_PATH.toString());
        body.put("claimGuardPath", CLAIM_GUARD_PATH.toString());
        body.put("bridgeExists", Files.exists(BRIDGE_PATH));
        body.put("claimGuardExists", Files.exists(CLAIM_GUARD_PATH));
        return body;
    }
}
""",
        encoding="utf-8",
    )


def write_it() -> None:
    IT_PATH.parent.mkdir(parents=True, exist_ok=True)

    IT_PATH.write_text(
        """package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17TrueAwareResultCardControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void summaryShouldExposeClaimSafeTrueAwareResultCard() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/true-aware/result-card/summary", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("status")).isEqualTo("consumer_ready");
        assertThat(body.get("claimLevel")).isEqualTo("single_true_v2a_candidate_available");
        assertThat(body.get("primaryCaseId")).isEqualTo("glass_drop_room_001");
        assertThat(body.get("primaryModel")).isEqualTo("MMAudio");
        assertThat(body.get("safeTrueMmaudioRecordCount")).isEqualTo(1);
        assertThat(body.get("rawCandidateRecordCount")).isEqualTo(9);
        assertThat(body.get("trueMmaudioSingleSuccess")).isEqualTo(true);
        assertThat(body.get("trueMmaudioBatchSuccess")).isEqualTo(false);
        assertThat(body.get("fullCandidateRankingAvailable")).isEqualTo(false);
        assertThat(body.get("productionSloVerified")).isEqualTo(false);
        assertThat(body.get("k6ThresholdPassVerified")).isEqualTo(false);
    }

    @Test
    void healthShouldConfirmArtifactBackedContract() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/true-aware/result-card/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("status")).isEqualTo("UP");
        assertThat(body.get("artifactBacked")).isEqualTo(true);
        assertThat(body.get("bridgeExists")).isEqualTo(true);
        assertThat(body.get("claimGuardExists")).isEqualTo(true);
        assertThat(body.get("claimLevel")).isEqualTo("single_true_v2a_candidate_available");
    }
}
""",
        encoding="utf-8",
    )


def write_doc_and_report(copied: dict[str, str], bridge: dict[str, Any]) -> None:
    DOC_PATH.parent.mkdir(parents=True, exist_ok=True)

    strict = bridge["strict_boundary"]
    card = bridge["platform_result_card"]

    report = {
        "schema_version": "week17.true_aware.java_api_report.v1",
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "source": "Mainbase true-aware platform bridge",
        "copied_artifacts": copied,
        "controller": str(CONTROLLER_PATH),
        "integration_test": str(IT_PATH),
        "endpoints": [
            "GET /api/week17/true-aware/result-card/health",
            "GET /api/week17/true-aware/result-card/summary",
            "GET /api/week17/true-aware/result-card/bridge",
            "GET /api/week17/true-aware/result-card/claim-guard",
            "GET /api/week17/true-aware/result-card/artifacts",
        ],
        "strict_boundary": strict,
        "platform_result_card": {
            "status": card.get("status"),
            "primary_case_id": card.get("primary_case_id"),
            "primary_model": card.get("primary_model"),
            "safe_true_mmaudio_record_count": card.get("safe_true_mmaudio_record_count"),
            "raw_candidate_record_count": card.get("raw_candidate_record_count"),
        },
        "explicit_non_claims": [
            "No true MMAudio batch success.",
            "No full candidate ranking claim.",
            "No production SLO claim.",
            "No k6 threshold pass claim.",
        ],
    }

    API_REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    DOC_PATH.write_text(
        f"""# Week17 True-aware Result Card API

This API exposes the Mainbase true-aware platform bridge as an artifact-backed Java result-card contract.

## Endpoints

- `GET /api/week17/true-aware/result-card/health`
- `GET /api/week17/true-aware/result-card/summary`
- `GET /api/week17/true-aware/result-card/bridge`
- `GET /api/week17/true-aware/result-card/claim-guard`
- `GET /api/week17/true-aware/result-card/artifacts`

## Claim boundary

- true MMAudio single success: `{strict.get("true_mmaudio_single_success")}`
- true MMAudio audio artifact count: `{strict.get("true_mmaudio_audio_artifact_count")}`
- true MMAudio batch success: `{strict.get("true_mmaudio_batch_success")}`
- full candidate ranking available: `{strict.get("full_candidate_ranking_available")}`
- production SLO verified: `{strict.get("production_slo_verified")}`
- k6 threshold pass verified: `{strict.get("k6_threshold_pass_verified")}`

## Platform behavior

Java must expose `safe_true_mmaudio_record_count=1` and keep raw candidate counts as context only.
The API must not inflate the single true V2A candidate into batch success.
""",
        encoding="utf-8",
    )


def main() -> None:
    bridge = load_json(SRC_BRIDGE)
    validate_bridge(bridge)

    copied = copy_required_inputs()
    write_controller()
    write_it()
    write_doc_and_report(copied, bridge)

    print("WROTE", CONTROLLER_PATH)
    print("WROTE", IT_PATH)
    print("WROTE", DOC_PATH)
    print("WROTE", API_REPORT_PATH)
    print("JAVA_TRUE_AWARE_RESULT_CARD_READY=1")
    print("SAFE_TRUE_MMAUDIO_RECORD_COUNT=1")
    print("RAW_CANDIDATE_RECORD_COUNT=", bridge["platform_result_card"]["raw_candidate_record_count"])


if __name__ == "__main__":
    main()