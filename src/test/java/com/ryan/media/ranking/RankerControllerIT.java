package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@SpringBootTest(
        classes = RankerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "ranker.mainbase-root=../audio_engineering_repo_skeleton_v1",
                "ranker.java-root=.",
                "logging.level.root=WARN"
        })
class RankerControllerIT {
    @Autowired TestRestTemplate http;

    @Test
    void importsRealBundleAndExposesRegistryMetricsAndArtifacts() {
        ResponseEntity<JsonNode> imported = http.postForEntity(
                "/api/rankers/import",
                Map.of("bundlePath", "artifacts/models/preference_ranker_v1/delivery"),
                JsonNode.class);
        assertEquals(200, imported.getStatusCode().value());
        JsonNode version = imported.getBody().path("version");
        assertEquals("DATA_BLOCKED", version.path("promotionStatus").asText());
        assertFalse(version.path("modelPresent").asBoolean());
        assertEquals(0, version.path("recommendationCount").asInt());
        String name = version.path("rankerVersion").asText();
        JsonNode reused = http.postForObject(
                "/api/rankers/import",
                Map.of("bundlePath", "artifacts/models/preference_ranker_v1/delivery"),
                JsonNode.class);
        assertEquals(true, reused.path("reused").asBoolean());
        assertEquals(1, http.getForObject("/api/rankers", JsonNode.class).size());
        assertEquals(name, http.getForObject(
                "/api/rankers/" + name, JsonNode.class).path("rankerVersion").asText());
        HttpHeaders cacheHeaders = new HttpHeaders();
        cacheHeaders.setIfNoneMatch("\"" + version.path("bundleDigest").asText() + "\"");
        assertEquals(304, http.exchange(
                "/api/rankers/" + name,
                HttpMethod.GET,
                new HttpEntity<>(cacheHeaders),
                Void.class).getStatusCode().value());
        assertEquals("DATA_BLOCKED", http.getForObject(
                "/api/rankers/" + name + "/metrics", JsonNode.class)
                .path("promotionStatus").asText());
        assertFalse(http.getForObject(
                "/api/rankers/" + name + "/artifacts", JsonNode.class)
                .path("storedBundlePath").asText().isBlank());
    }
}
