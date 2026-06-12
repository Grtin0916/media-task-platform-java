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
class Week15TemporalAlignmentRemediationIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void temporalAlignmentRemediationApiShouldExposeMainbaseRegressionEvidence() throws Exception {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/week15/temporal-alignment-remediation",
            Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("PASS");
        assertThat(body.get("gateDecision")).isEqualTo("TEMPORAL_ALIGNMENT_REMEDIATION_REGRESSION_GUARDED");

        Map<?, ?> original = (Map<?, ?>) body.get("original");
        Map<?, ?> remediated = (Map<?, ?>) body.get("remediated");
        Map<?, ?> improvement = (Map<?, ?>) body.get("improvement");

        assertThat(original.get("candidateCount")).isEqualTo(10);
        assertThat(original.get("failCount")).isEqualTo(2);
        assertThat(original.get("eventLocalPassCount")).isEqualTo(3);

        assertThat(remediated.get("candidateCount")).isEqualTo(10);
        assertThat(remediated.get("failCount")).isEqualTo(0);
        assertThat(remediated.get("eventLocalPassCount")).isEqualTo(5);

        assertThat(improvement.get("eventLocalPassDelta")).isEqualTo(2);
        assertThat(improvement.get("failCountDelta")).isEqualTo(-2);
        assertThat(improvement.get("remediatedCandidateIds").toString())
            .contains("procedural_v0_0004")
            .contains("procedural_v0_0010");

        Map<String, Object> reportPayload = new LinkedHashMap<>();
        reportPayload.put("schemaVersion", "week15.temporal_alignment_remediation_api_contract_report.v1");
        reportPayload.put("status", "PASS");
        reportPayload.put("endpoint", "/api/week15/temporal-alignment-remediation");
        reportPayload.put("testsRun", 1);
        reportPayload.put("failures", 0);
        reportPayload.put("errors", 0);
        reportPayload.put("candidateCount", remediated.get("candidateCount"));
        reportPayload.put("originalFailCount", original.get("failCount"));
        reportPayload.put("remediatedFailCount", remediated.get("failCount"));
        reportPayload.put("originalEventLocalPassCount", original.get("eventLocalPassCount"));
        reportPayload.put("remediatedEventLocalPassCount", remediated.get("eventLocalPassCount"));
        reportPayload.put("eventLocalPassDelta", improvement.get("eventLocalPassDelta"));
        reportPayload.put("failCountDelta", improvement.get("failCountDelta"));
        reportPayload.put("remediatedCandidateIds", improvement.get("remediatedCandidateIds"));
        reportPayload.put("boundary", body.get("boundary"));

        Path report = Path.of("artifacts/manifests/week15_temporal_alignment_remediation_api_contract_report.json");
        Files.createDirectories(report.getParent());
        Files.writeString(
            report,
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportPayload)
                + System.lineSeparator()
        );
    }
}