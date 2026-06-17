package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week15TemporalAlignmentReviewStateRegistryBackedIT {
    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void reviewStateIsArtifactRegistryBackedAndKeepsClaimBoundary() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/week15/temporal-alignment-review-state",
                String.class
        );

        assertEquals(200, response.getStatusCode().value());
        String body = response.getBody();
        assertTrue(body != null && !body.isBlank(), "response body must not be blank");

        JsonNode root = mapper.readTree(body);
        assertEquals("ARTIFACT_REGISTRY_BACKED", root.path("sourceType").asText());
        assertTrue(root.path("artifactRegistryBacked").asBoolean(), "artifactRegistryBacked must be true");
        assertTrue(body.contains("procedural_v0_0004"), "riskCandidateIds must include procedural_v0_0004");
        assertTrue(
                body.contains("HUMAN_REVIEW_PARTIAL")
                        || body.contains("NOT_PERFORMED")
                        || body.contains("NOT_CLAIMED"),
                "review-state must keep honest claim boundary"
        );
        assertTrue(
                body.contains("artifacts/manifests/week15_temporal_alignment_semantic_quality_e2e_manifest.json"),
                "response must expose E2E manifest artifact link"
        );
    }
}
