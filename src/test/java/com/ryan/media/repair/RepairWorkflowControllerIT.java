package com.ryan.media.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = RepairWorkflowTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "repair.mainbase-root=../audio_engineering_repo_skeleton_v1",
                "repair.java-root=.",
                "repair.source-commit=b1e19c1",
                "debug=false",
                "logging.level.root=INFO",
                "logging.level.org.springframework.web=INFO"
        })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepairWorkflowControllerIT {
    @Autowired
    private TestRestTemplate restTemplate;

    private static String batchId;
    private static String manualReviewId;
    private static String rejectedId;

    @Test
    @Order(1)
    void rejectsMissingHandoffAndPathTraversalWithoutPartialImport() {
        ResponseEntity<JsonNode> missing = restTemplate.postForEntity(
                "/api/repair-workflows/import",
                Map.of("handoffPath", "artifacts/manifests/not-found.json"),
                JsonNode.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, missing.getStatusCode());
        assertEquals(
                "REPAIR_ARTIFACT_IMPORT_REJECTED",
                missing.getBody().path("code").asText());

        ResponseEntity<JsonNode> traversal = restTemplate.postForEntity(
                "/api/repair-workflows/import",
                Map.of("handoffPath", "../media-task-platform-java/pom.xml"),
                JsonNode.class);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, traversal.getStatusCode());
        assertEquals(
                "REPAIR_ARTIFACT_IMPORT_REJECTED",
                traversal.getBody().path("code").asText());

        JsonNode records = restTemplate.getForObject("/api/repair-records", JsonNode.class);
        assertEquals(0, records.size());
    }

    @Test
    @Order(2)
    void importsTwentyRealRecordsAndPreservesUpstreamDecisions() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/repair-workflows/import", null, JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(20, response.getBody().path("recordCount").asInt());
        assertFalse(response.getBody().path("reused").asBoolean());
        batchId = response.getBody().path("batchId").asText();

        JsonNode records = restTemplate.getForObject("/api/repair-records", JsonNode.class);
        assertEquals(20, records.size());
        Map<String, Integer> decisions = new HashMap<>();
        for (JsonNode record : records) {
            decisions.merge(record.path("repairDecision").asText(), 1, Integer::sum);
            assertEquals("b1e19c1", record.path("sourceCommit").asText());
            assertEquals("SHA256_VERIFIED", record.path("before").path("integrityStatus").asText());
            assertEquals("SHA256_VERIFIED", record.path("after").path("integrityStatus").asText());
            assertEquals(64, record.path("before").path("sha256").asText().length());
            assertTrue(record.path("before").path("sizeBytes").asLong() > 0);
            if ("MANUAL_REVIEW".equals(record.path("repairDecision").asText())) {
                manualReviewId = record.path("repairId").asText();
            } else if ("REPAIR_REJECTED".equals(record.path("repairDecision").asText())) {
                rejectedId = record.path("repairId").asText();
            }
        }
        assertEquals(18, decisions.get("MANUAL_REVIEW"));
        assertEquals(2, decisions.get("REPAIR_REJECTED"));
        assertNotNull(manualReviewId);
        assertNotNull(rejectedId);
    }

    @Test
    @Order(3)
    void repeatedImportIsIdempotentAndSummaryMatchesApi() {
        ResponseEntity<JsonNode> repeated = restTemplate.postForEntity(
                "/api/repair-workflows/import", null, JsonNode.class);
        assertEquals(20, repeated.getBody().path("recordCount").asInt());
        assertTrue(repeated.getBody().path("reused").asBoolean());
        assertEquals(batchId, repeated.getBody().path("batchId").asText());

        JsonNode summary = restTemplate.getForObject(
                "/api/repair-workflows/" + batchId + "/summary", JsonNode.class);
        assertEquals(20, summary.path("recordCount").asInt());
        assertEquals(18, summary.path("decisionCounts").path("MANUAL_REVIEW").asInt());
        assertEquals(2, summary.path("decisionCounts").path("REPAIR_REJECTED").asInt());
        assertEquals(18, summary.path("manualReviewPendingCount").asInt());
        assertEquals(0, summary.path("manualReviewCompletedCount").asInt());
    }

    @Test
    @Order(4)
    void detailHistoryAndEtagAreAvailableOverHttp() {
        ResponseEntity<JsonNode> detail = restTemplate.getForEntity(
                "/api/repair-records/" + manualReviewId, JsonNode.class);
        assertEquals(HttpStatus.OK, detail.getStatusCode());
        assertEquals(
                "\"repair-" + manualReviewId + "-v0\"",
                detail.getHeaders().getETag());
        JsonNode history = restTemplate.getForObject(
                "/api/repair-records/" + manualReviewId + "/history", JsonNode.class);
        assertEquals(1, history.size());
        assertEquals("MANUAL_REVIEW", history.get(0).path("decision").asText());
    }

    @Test
    @Order(5)
    void rejectsIllegalPromotionIncompleteReviewAndStaleVersion() {
        Map<String, Object> complete = review(0L, "FINAL_SELECTED");
        ResponseEntity<JsonNode> rejectedPromotion = restTemplate.postForEntity(
                "/api/repair-records/" + rejectedId + "/reviews",
                complete,
                JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, rejectedPromotion.getStatusCode());

        Map<String, Object> incomplete = new HashMap<>(complete);
        incomplete.put("reason", "");
        ResponseEntity<JsonNode> incompleteResponse = restTemplate.postForEntity(
                "/api/repair-records/" + manualReviewId + "/reviews",
                incomplete,
                JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, incompleteResponse.getStatusCode());

        HttpHeaders staleHeaders = new HttpHeaders();
        staleHeaders.set("If-Match", "\"repair-" + manualReviewId + "-v99\"");
        ResponseEntity<JsonNode> stale = restTemplate.exchange(
                "/api/repair-records/" + manualReviewId + "/reviews",
                HttpMethod.POST,
                new HttpEntity<>(review(99L, "RUNNER_UP"), staleHeaders),
                JsonNode.class);
        assertEquals(HttpStatus.PRECONDITION_FAILED, stale.getStatusCode());
        assertEquals("STALE_REVIEW_VERSION", stale.getBody().path("code").asText());
    }

    @Test
    @Order(6)
    void generatedEvidenceMatchesTheUnmodifiedWorkflow() throws Exception {
        JsonNode report = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                Path.of("artifacts/manifests/repair_workflow_report_20260716.json").toFile());
        assertEquals(20, report.path("summary").path("recordCount").asInt());
        assertEquals(18, report.path("summary").path("decisionCounts")
                .path("MANUAL_REVIEW").asInt());
        assertEquals(2, report.path("summary").path("decisionCounts")
                .path("REPAIR_REJECTED").asInt());
        assertEquals(0, report.path("summary").path("manualReviewCompletedCount").asInt());
        assertTrue(Files.size(Path.of(
                "artifacts/manifests/repair_artifact_index_20260716.json")) > 0);
        assertEquals(
                7,
                Files.readAllLines(Path.of(
                        "artifacts/week19/repair_manual_reviews_20260716.csv")).size());
    }

    private static Map<String, Object> review(long version, String targetDecision) {
        Map<String, Object> body = new HashMap<>();
        body.put("preference", "A");
        body.put("reason", "human supplied reason");
        body.put("confidence", 0.8);
        body.put("audibleArtifact", "none");
        body.put("forbiddenEventStatus", "absent");
        body.put("reviewedBy", "human-reviewer");
        body.put("targetDecision", targetDecision);
        body.put("reviewVersion", version);
        return body;
    }
}
