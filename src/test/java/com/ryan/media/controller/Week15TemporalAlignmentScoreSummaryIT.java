package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week15TemporalAlignmentScoreSummaryIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @SuppressWarnings("unchecked")
    void temporalAlignmentScoreSummaryApiShouldExposeDashboardReadyEvidence() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/week15/temporal-alignment-score-summary",
            Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("PASS");
        assertThat(body.get("gateDecision"))
            .isEqualTo("TEMPORAL_ALIGNMENT_DASHBOARD_READY_AND_REGRESSION_GUARDED");

        Map<String, Object> scoreSummary = (Map<String, Object>) body.get("scoreSummary");
        Map<String, Object> remediationDelta = (Map<String, Object>) body.get("remediationDelta");
        Map<String, Object> artifactLinks = (Map<String, Object>) body.get("artifactLinks");
        Map<String, Object> source = (Map<String, Object>) body.get("source");

        assertThat(scoreSummary.get("candidateCount")).isEqualTo(10);
        assertThat(scoreSummary.get("originalFailCount")).isEqualTo(2);
        assertThat(scoreSummary.get("remediatedFailCount")).isEqualTo(0);
        assertThat(scoreSummary.get("originalEventLocalPassCount")).isEqualTo(3);
        assertThat(scoreSummary.get("remediatedEventLocalPassCount")).isEqualTo(5);

        assertThat(source.get("javaInputHead")).isEqualTo("d09b4f5");
        assertThat(source).doesNotContainKey("javaHead");

        assertThat(remediationDelta.get("eventLocalPassDelta")).isEqualTo(2);
        assertThat(remediationDelta.get("failCountDelta")).isEqualTo(-2);
        assertThat(remediationDelta.get("remediatedCandidateIds").toString())
            .contains("procedural_v0_0004")
            .contains("procedural_v0_0010");

        assertThat(body.get("candidateDrifts").toString())
            .contains("FAIL_DRIFT")
            .contains("procedural_v0_0004")
            .contains("procedural_v0_0010")
            .contains("trim_leading_low_energy_with_20ms_preroll");

        assertThat(artifactLinks.get("mainbaseDiagnostics").toString())
            .contains("week15_temporal_alignment_diagnostics.json");
        assertThat(artifactLinks.get("cloudReadyReport").toString())
            .contains("week15_temporal_alignment_dashboard_ready.json");

        Map<String, Object> reportPayload = new LinkedHashMap<>();
        reportPayload.put("schemaVersion", "week15.temporal_alignment_score_summary_contract_report.v1");
        reportPayload.put("status", "PASS");
        reportPayload.put("endpoint", "/api/week15/temporal-alignment-score-summary");
        reportPayload.put("testsRun", 1);
        reportPayload.put("failures", 0);
        reportPayload.put("errors", 0);
        reportPayload.put("gateDecision", body.get("gateDecision"));
        reportPayload.put("source", body.get("source"));
        reportPayload.put("scoreSummary", scoreSummary);
        reportPayload.put("remediationDelta", remediationDelta);
        reportPayload.put("candidateDrifts", body.get("candidateDrifts"));
        reportPayload.put("artifactLinks", artifactLinks);
        reportPayload.put("boundary", body.get("boundary"));

        Path report = Path.of("artifacts/manifests/week15_java_temporal_alignment_score_summary_contract_report.json");
        Files.createDirectories(report.getParent());
        Files.writeString(
            report,
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportPayload) + System.lineSeparator()
        );
    }
}
