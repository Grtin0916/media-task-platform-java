package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
        classes = RankerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"logging.level.root=WARN"})
class RankerVersionConflictIT {
    private static final Path ROOT = createRoot();
    @Autowired TestRestTemplate http;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ranker.mainbase-root", () -> ROOT.toString());
        registry.add("ranker.java-root", () -> ROOT.resolve("java").toString());
    }

    @BeforeAll
    static void bundles() throws Exception {
        RankerTestBundleFactory.create(
                ROOT.resolve("one"), "same-version", "a".repeat(64),
                RankerPromotionStatus.DATA_BLOCKED, false, false, 0);
        RankerTestBundleFactory.create(
                ROOT.resolve("two"), "same-version", "b".repeat(64),
                RankerPromotionStatus.DATA_BLOCKED, false, false, 0);
        Path digestMismatch = RankerTestBundleFactory.create(
                ROOT.resolve("digest-mismatch"), "digest-mismatch", "c".repeat(64),
                RankerPromotionStatus.DATA_BLOCKED, false, false, 0);
        Files.writeString(digestMismatch.resolve("model-card.json"), "tampered");
        RankerTestBundleFactory.create(
                ROOT.resolve("invalid-blocked"), "invalid-blocked", "d".repeat(64),
                RankerPromotionStatus.DATA_BLOCKED, true, false, 0);
    }

    @Test
    void digestPromotionAndPathErrorsUseStableProblemDetails() {
        ResponseEntity<JsonNode> digest = post("digest-mismatch");
        assertEquals(422, digest.getStatusCode().value());
        assertEquals(
                "RANKER_ARTIFACT_DIGEST_MISMATCH",
                digest.getBody().path("code").asText());

        ResponseEntity<JsonNode> invariant = post("invalid-blocked");
        assertEquals(422, invariant.getStatusCode().value());
        assertEquals("INVALID_BLOCKED_BUNDLE", invariant.getBody().path("code").asText());

        ResponseEntity<JsonNode> traversal = post("../outside");
        assertEquals(400, traversal.getStatusCode().value());
        assertEquals(
                "RANKER_PATH_OUTSIDE_ALLOWED_ROOT",
                traversal.getBody().path("code").asText());
    }

    @Test
    void sameVersionDifferentDigestReturnsProblemDetail409() {
        assertEquals(200, post("one").getStatusCode().value());
        ResponseEntity<JsonNode> conflict = post("two");
        assertEquals(409, conflict.getStatusCode().value());
        assertEquals("RANKER_VERSION_CONFLICT", conflict.getBody().path("code").asText());
        assertEquals("same-version", conflict.getBody().path("rankerVersion").asText());
    }

    private ResponseEntity<JsonNode> post(String path) {
        return http.postForEntity(
                "/api/rankers/import", Map.of("bundlePath", path), JsonNode.class);
    }

    private static Path createRoot() {
        try { return java.nio.file.Files.createTempDirectory("ranker-conflict-it-"); }
        catch (IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
}
