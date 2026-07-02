package com.ryan.media.controller;

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
