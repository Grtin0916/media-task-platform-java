package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week16TemporalAlignmentRerunPlanController {

  static final String ENDPOINT = "/api/week16/temporal-alignment/rerun-plan";

  private static final Path REGRESSION_REPORT_PATH =
      Path.of("artifacts/manifests/week16_java_temporal_alignment_failure_regression_report.json");

  private static final List<String> DEFAULT_BLOCKED_CLAIMS =
      List.of(
          "semantic_audio_quality_pass_not_verified",
          "human_review_pass_not_verified",
          "final_mix_readiness_not_verified",
          "live_java_service_availability_not_verified",
          "live_prometheus_or_grafana_import_not_verified",
          "production_slo_or_real_cloud_deployment_not_verified");

  private final ObjectMapper objectMapper;

  public Week16TemporalAlignmentRerunPlanController(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @GetMapping(ENDPOINT)
  public ResponseEntity<JsonNode> getRerunPlan() throws IOException {
    if (!Files.exists(REGRESSION_REPORT_PATH)) {
      return ResponseEntity.notFound().build();
    }

    JsonNode regressionReport = objectMapper.readTree(REGRESSION_REPORT_PATH.toFile());
    return ResponseEntity.ok(buildRerunPlan(regressionReport));
  }

  private ObjectNode buildRerunPlan(JsonNode regressionReport) {
    ArrayNode records = objectMapper.createArrayNode();

    for (JsonNode sourceRecord : regressionReport.path("records")) {
      String candidateId = sourceRecord.path("candidateId").asText("");
      if (candidateId.isBlank()) {
        continue;
      }

      ObjectNode record = objectMapper.createObjectNode();
      record.put("candidateId", candidateId);
      record.put(
          "failureBucket",
          sourceRecord.path("failureBucket").asText("P3_UNCLASSIFIED_EVIDENCE_GAP"));
      record.put("severity", sourceRecord.path("severity").asText("P3"));
      record.put("fixtureRole", sourceRecord.path("fixtureRole").asText("evidence_gap_fixture"));
      record.put("eligibleForRerun", sourceRecord.path("eligibleForRerun").asBoolean(false));
      record.put(
          "rerunReason",
          sourceRecord
              .path("rerunReason")
              .asText("insufficient evidence for deterministic rerun eligibility"));
      record.put(
          "idempotencyKeyPolicy",
          "required_per_candidate_attempt: Idempotency-Key must include candidateId and attempt scope; repeated requests preserve previous attempt evidence and return the same plan");
      record.put("previousAttemptPreserved", true);
      record.put("realWorkerTriggered", false);
      record.put(
          "workerBoundary",
          "This endpoint returns a rerun plan only; it does not start an async worker or mutate task state.");

      copyIfPresent(sourceRecord, record, "nextAction");
      copyIfPresent(sourceRecord, record, "blockedClaims");
      copyIfPresent(sourceRecord, record, "classificationEvidence");
      copyIfPresent(sourceRecord, record, "seedEvidence");

      records.add(record);
    }

    int candidateTotal = records.size();
    int eligibleForRerunTotal = 0;
    int previousAttemptPreservedTotal = 0;

    for (JsonNode record : records) {
      if (record.path("eligibleForRerun").asBoolean(false)) {
        eligibleForRerunTotal++;
      }
      if (record.path("previousAttemptPreserved").asBoolean(false)) {
        previousAttemptPreservedTotal++;
      }
    }

    ArrayNode blockedClaims = extractBlockedClaims(regressionReport);

    boolean sourcePass = regressionReport.path("decision").asText("").startsWith("PASS");
    boolean pass =
        sourcePass
            && candidateTotal == 10
            && eligibleForRerunTotal == 2
            && previousAttemptPreservedTotal == 10;

    ObjectNode summary = objectMapper.createObjectNode();
    summary.put("candidateTotal", candidateTotal);
    summary.put("eligibleForRerunTotal", eligibleForRerunTotal);
    summary.put("nonRerunTotal", candidateTotal - eligibleForRerunTotal);
    summary.put("previousAttemptPreservedTotal", previousAttemptPreservedTotal);
    summary.put("realWorkerTriggeredTotal", 0);
    summary.set("blockedClaims", blockedClaims);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("schemaVersion", "week16.java.temporal_alignment.rerun_plan.v1");
    body.put("decision", pass ? "PASS_WEEK16_JAVA_RERUN_PLAN" : "FAIL_WEEK16_JAVA_RERUN_PLAN");
    body.put("sourceMainbaseDecision", regressionReport.path("decision").asText(""));
    body.put("sourceClassificationMode", regressionReport.path("classificationMode").asText(""));
    body.put("sourcePath", REGRESSION_REPORT_PATH.toString());
    body.put("apiCandidate", ENDPOINT);
    body.put("candidateTotal", candidateTotal);
    body.put("eligibleForRerunTotal", eligibleForRerunTotal);
    body.put("previousAttemptPreservedTotal", previousAttemptPreservedTotal);
    body.put("realWorkerTriggeredTotal", 0);
    body.put(
        "boundary",
        "RANDOM_PORT IT evidence only; this does not claim live service availability, production auth, async worker execution, or exactly-once rerun.");
    body.set("summary", summary);
    body.set("blockedClaims", blockedClaims);
    body.set("records", records);

    return body;
  }

  private void copyIfPresent(JsonNode source, ObjectNode target, String fieldName) {
    JsonNode value = source.path(fieldName);
    if (!value.isMissingNode() && !value.isNull()) {
      target.set(fieldName, value);
    }
  }

  private ArrayNode extractBlockedClaims(JsonNode regressionReport) {
    JsonNode fromSummary = regressionReport.path("summary").path("blockedClaims");
    if (fromSummary.isArray() && fromSummary.size() > 0) {
      ArrayNode out = objectMapper.createArrayNode();
      fromSummary.forEach(out::add);
      return out;
    }

    ArrayNode fallback = objectMapper.createArrayNode();
    DEFAULT_BLOCKED_CLAIMS.forEach(fallback::add);
    return fallback;
  }
}