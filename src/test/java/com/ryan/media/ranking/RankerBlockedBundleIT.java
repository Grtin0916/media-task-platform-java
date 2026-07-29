package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(
        classes = RankerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ranker.mainbase-root=../audio_engineering_repo_skeleton_v1",
                "ranker.java-root=target/ranker-blocked-it",
                "logging.level.root=WARN"
        })
class RankerBlockedBundleIT {
    @Autowired TestRestTemplate http;

    @Test
    void blockedBundleReturnsUnavailableWithoutChangingPublishDecision() {
        http.postForEntity(
                "/api/rankers/import",
                Map.of("bundlePath", "artifacts/models/preference_ranker_v1/delivery"),
                JsonNode.class);
        JsonNode result = http.getForObject(
                "/api/ranker-results/fb_001_tuesday_repair", JsonNode.class);
        assertEquals("DATA_BLOCKED", result.path("rankerPromotionStatus").asText());
        assertEquals("UNAVAILABLE", result.path("recommendationStatus").asText());
        assertEquals("NOT_STARTED", result.path("humanReviewStatus").asText());
        assertEquals("PROVISIONAL_SELECTED", result.path("publishDecision").asText());
        assertEquals("HUMAN_LABELS_NOT_SUBMITTED", result.path("blockedReason").asText());
        assertEquals(0, result.path("finalSelectedMutationCount").asInt());
        assertEquals(0, http.getForObject(
                "/api/ranker-results/fb_001_tuesday_repair/history", JsonNode.class).size());
        JsonNode comparison = http.getForObject(
                "/api/ranker-results/fb_001_tuesday_repair/compare"
                        + "?from=preference-ranker-v1-20260730"
                        + "&to=preference-ranker-v1-20260730",
                JsonNode.class);
        assertEquals(false, comparison.path("changed").asBoolean());
    }
}
