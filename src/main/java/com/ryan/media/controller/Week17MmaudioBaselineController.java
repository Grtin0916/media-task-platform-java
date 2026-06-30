package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/week17/mmaudio-baseline")
public class Week17MmaudioBaselineController {

    private final ObjectMapper objectMapper;

    private final Path payloadPath = Path.of(
            "artifacts",
            "manifests",
            "week17_mmaudio_baseline",
            "mainbase_mmaudio_baseline_java_seed_payload.json"
    ).toAbsolutePath().normalize();

    public Week17MmaudioBaselineController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<JsonNode> payload() throws IOException {
        return ResponseEntity.ok(readPayload());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() throws IOException {
        JsonNode payload = readPayload();
        JsonNode boundary = payload.path("boundary");
        JsonNode claimBoundary = boundary.path("claim_boundary");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "PASS_JAVA_MMAUDIO_BASELINE_API_READY");
        body.put("source", payload.path("source").asText("mainbase"));
        body.put("caseCount", boundary.path("case_count").asInt(-1));
        body.put("candidateCount", boundary.path("candidate_count").asInt(-1));
        body.put("winnerCount", boundary.path("winner_count").asInt(-1));
        body.put("rejectedCount", boundary.path("rejected_count").asInt(-1));
        body.put("repairQueueCount", boundary.path("repair_queue_count").asInt(-1));
        body.put("badPromptCount", boundary.path("bad_prompt_count").asInt(-1));
        body.put("trueMmaudioGeneratedCount", boundary.path("true_mmaudio_generated_count").asInt(-1));
        body.put("allOutputsAreFallback", boundary.path("all_outputs_are_fallback").asBoolean(false));
        body.put("canClaimReadableCandidateAudio", claimBoundary.path("can_claim_readable_candidate_audio").asBoolean(false));
        body.put("canClaimDssConditionedControlBaseline", claimBoundary.path("can_claim_dss_conditioned_control_baseline").asBoolean(false));
        body.put("canClaimTrueMmaudioV2aSuccess", claimBoundary.path("can_claim_true_mmaudio_v2a_success").asBoolean(true));
        body.put("canClaimVideoSynchronizedQuality", claimBoundary.path("can_claim_video_synchronized_quality").asBoolean(true));
        body.put("payloadPath", payloadPath.toString());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/winners")
    public ResponseEntity<JsonNode> winners() throws IOException {
        return ResponseEntity.ok(readPayload().path("winners"));
    }

    @GetMapping("/repair-queue")
    public ResponseEntity<JsonNode> repairQueue() throws IOException {
        return ResponseEntity.ok(readPayload().path("repair_queue"));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", Files.isRegularFile(payloadPath) ? "UP" : "MISSING_PAYLOAD");
        body.put("payloadExists", Files.isRegularFile(payloadPath));
        body.put("payloadPath", payloadPath.toString());
        return ResponseEntity.ok(body);
    }

    private JsonNode readPayload() throws IOException {
        if (!Files.isRegularFile(payloadPath)) {
            throw new IOException("Payload file not found: " + payloadPath);
        }
        return objectMapper.readTree(payloadPath.toFile());
    }
}
