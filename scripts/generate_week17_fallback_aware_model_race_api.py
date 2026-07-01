#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(".")
MAINBASE = Path.home() / "work" / "audio_engineering_repo_skeleton_v1"

ARTIFACT_DIR = ROOT / "artifacts" / "manifests" / "week17_fallback_aware_model_race"
PAYLOAD_SRC = MAINBASE / "reports" / "week17_model_race_java_payload_20260701.json"
PAYLOAD_DST = ARTIFACT_DIR / "mainbase_week17_model_race_java_payload_20260701.json"
REPORT_PATH = ARTIFACT_DIR / "week17_fallback_aware_model_race_api_report.json"

CONTROLLER = ROOT / "src" / "main" / "java" / "com" / "ryan" / "media" / "controller" / "Week17FallbackAwareModelRaceController.java"
IT = ROOT / "src" / "test" / "java" / "com" / "ryan" / "media" / "controller" / "Week17FallbackAwareModelRaceControllerIT.java"
SUREFIRE_TXT = ROOT / "target" / "surefire-reports" / "com.ryan.media.controller.Week17FallbackAwareModelRaceControllerIT.txt"


CONTROLLER_CODE = r'''package com.ryan.media.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/week17/fallback-aware-model-race")
public class Week17FallbackAwareModelRaceController {

    private static final Path PAYLOAD_PATH = Path.of(
            "artifacts",
            "manifests",
            "week17_fallback_aware_model_race",
            "mainbase_week17_model_race_java_payload_20260701.json"
    );

    private final ObjectMapper objectMapper;

    public Week17FallbackAwareModelRaceController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/result")
    public ResponseEntity<Map<String, Object>> result() throws IOException {
        if (!Files.exists(PAYLOAD_PATH)) {
            return ResponseEntity.status(HttpStatus.FAILED_DEPENDENCY).body(Map.of(
                    "status", "missing_mainbase_payload",
                    "payload_path", PAYLOAD_PATH.toString()
            ));
        }

        Map<String, Object> payload = objectMapper.readValue(
                PAYLOAD_PATH.toFile(),
                new TypeReference<Map<String, Object>>() {}
        );

        payload.put("java_consumer_status", "consumed");
        payload.put("claim_boundary", Map.of(
                "true_mmaudio_v2a_success", false,
                "fallback_aware_reranker_ready", true,
                "production_slo_claim", false
        ));
        payload.put("payload_path", PAYLOAD_PATH.toString());

        return ResponseEntity.ok(payload);
    }
}
'''


IT_CODE = r'''package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17FallbackAwareModelRaceControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesFallbackAwareModelRaceResultFromMainbasePayload() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/fallback-aware-model-race/result",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("artifact_type")).isEqualTo("week17_fallback_aware_model_race_result");
        assertThat(body.get("true_mmaudio_status")).isEqualTo("blocked_by_torch_torchaudio_abi");
        assertThat(((Number) body.get("case_count")).intValue()).isEqualTo(6);
        assertThat(((Number) body.get("winner_count")).intValue()).isEqualTo(6);
        assertThat(((Number) body.get("canonical_candidate_count")).intValue()).isEqualTo(28);
        assertThat(body.get("java_consumer_status")).isEqualTo("consumed");

        Object items = body.get("items");
        assertThat(items).isInstanceOf(List.class);
        assertThat((List<?>) items).hasSize(6);
    }
}
'''


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def write_report(status: str, test_status: str | None = None) -> None:
    payload = load_json(PAYLOAD_DST) if PAYLOAD_DST.exists() else {}

    surefire_exists = SUREFIRE_TXT.exists()
    surefire_tail = ""
    if surefire_exists:
        text = SUREFIRE_TXT.read_text(encoding="utf-8", errors="replace")
        surefire_tail = "\n".join(text.splitlines()[-20:])

    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "status": status,
        "test_status": test_status or ("passed" if "Failures: 0, Errors: 0" in surefire_tail else "not_run_or_unknown"),
        "api_path": "/api/week17/fallback-aware-model-race/result",
        "controller": str(CONTROLLER),
        "integration_test": str(IT),
        "payload": str(PAYLOAD_DST),
        "artifact_type": payload.get("artifact_type"),
        "true_mmaudio_status": payload.get("true_mmaudio_status"),
        "case_count": payload.get("case_count"),
        "winner_count": payload.get("winner_count"),
        "canonical_candidate_count": payload.get("canonical_candidate_count"),
        "claim_boundary": {
            "true_mmaudio_v2a_success": False,
            "fallback_aware_reranker_ready": payload.get("winner_count") == 6,
            "production_slo_claim": False
        },
        "surefire_report": str(SUREFIRE_TXT),
        "surefire_exists": surefire_exists,
        "surefire_tail": surefire_tail
    }

    REPORT_PATH.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))


def generate() -> None:
    if not PAYLOAD_SRC.exists():
        raise FileNotFoundError(f"Missing Mainbase payload: {PAYLOAD_SRC}")

    ARTIFACT_DIR.mkdir(parents=True, exist_ok=True)
    CONTROLLER.parent.mkdir(parents=True, exist_ok=True)
    IT.parent.mkdir(parents=True, exist_ok=True)

    shutil.copy2(PAYLOAD_SRC, PAYLOAD_DST)

    payload = load_json(PAYLOAD_DST)
    assert payload.get("artifact_type") == "week17_fallback_aware_model_race_result", payload
    assert payload.get("true_mmaudio_status") == "blocked_by_torch_torchaudio_abi", payload
    assert int(payload.get("case_count", 0)) == 6, payload
    assert int(payload.get("winner_count", 0)) == 6, payload

    CONTROLLER.write_text(CONTROLLER_CODE, encoding="utf-8")
    IT.write_text(IT_CODE, encoding="utf-8")
    write_report("generated")


def finalize() -> None:
    write_report("finalized")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--finalize", action="store_true")
    args = parser.parse_args()

    if args.finalize:
        finalize()
    else:
        generate()


if __name__ == "__main__":
    main()