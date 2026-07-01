package com.ryan.media.controller;

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
