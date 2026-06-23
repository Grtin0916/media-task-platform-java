package com.ryan.media.controller;

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
