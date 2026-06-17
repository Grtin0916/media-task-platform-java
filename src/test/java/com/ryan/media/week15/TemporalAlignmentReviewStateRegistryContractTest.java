package com.ryan.media.week15;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalAlignmentReviewStateRegistryContractTest {
    @Test
    void loadsArtifactRegistryBackedReviewStateWithoutStartingSpringBootServer() {
        TemporalAlignmentReviewStateRegistry registry = new TemporalAlignmentReviewStateRegistry();

        Map<String, Object> state = registry.loadReviewState();

        assertEquals("ARTIFACT_REGISTRY_BACKED", state.get("sourceType"));
        assertEquals(Boolean.TRUE, state.get("artifactRegistryBacked"));
        assertEquals("artifact-registry-snapshot", state.get("source"));

        Object ids = state.get("riskCandidateIds");
        assertTrue(ids instanceof List<?>, "riskCandidateIds must be a list");
        assertTrue(((List<?>) ids).contains("procedural_v0_0004"),
                "riskCandidateIds must contain procedural_v0_0004");

        Object boundary = state.get("reviewBoundary");
        assertTrue(boundary instanceof Map<?, ?>, "reviewBoundary must be a map");
        String boundaryText = boundary.toString();
        assertTrue(
                boundaryText.contains("HUMAN_REVIEW_PARTIAL")
                        || boundaryText.contains("NOT_PERFORMED")
                        || boundaryText.contains("NOT_CLAIMED"),
                "claim boundary must remain honest"
        );

        Object links = state.get("artifactLinks");
        assertTrue(links instanceof Map<?, ?>, "artifactLinks must be a map");
        assertTrue(links.toString().contains("week15_temporal_alignment_semantic_quality_e2e_manifest.json"),
                "artifact links must expose E2E manifest path");

        assertNotNull(state.get("e2eManifestSchemaVersion"));
        assertEquals("SEMANTIC_REVIEW_READY", state.get("qualityGateLiteStatus"));
    }
}
