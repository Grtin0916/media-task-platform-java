package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17DemoReleaseHandoffControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesMainbaseDemoReleaseHandoff() {
        String url = "http://localhost:" + port + "/api/week17/demo-release-handoff";

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("contractVersion").asText())
                .isEqualTo("week17-demo-release-handoff-v1");
        assertThat(body.path("releaseHandoffReady").asBoolean()).isTrue();

        JsonNode audioDemo = body.path("audioDemo");
        assertThat(audioDemo.path("safeTrueMmaudioRecordCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(audioDemo.path("zipValid").asBoolean()).isTrue();
        assertThat(audioDemo.path("zipContainsIndex").asBoolean()).isTrue();
        assertThat(audioDemo.path("zipContainsWav").asBoolean()).isTrue();

        JsonNode claimBoundary = body.path("claimBoundary");
        assertThat(claimBoundary.path("boundaryPreserved").asBoolean()).isTrue();
        assertThat(claimBoundary.path("trueMmaudioBatchSuccess").asBoolean()).isFalse();
        assertThat(claimBoundary.path("fullCandidateRankingAvailable").asBoolean()).isFalse();
        assertThat(claimBoundary.path("productionSloVerified").asBoolean()).isFalse();
        assertThat(claimBoundary.path("k6ThresholdPassVerified").asBoolean()).isFalse();
        assertThat(claimBoundary.path("liveGrafanaImportVerified").asBoolean()).isFalse();

        JsonNode javaApi = body.path("javaApi");
        assertThat(javaApi.path("endpoint").asText())
                .isEqualTo("/api/week17/demo-release-handoff");
        assertThat(javaApi.path("randomPortITOnly").asBoolean()).isTrue();
        assertThat(javaApi.path("liveServiceAvailabilityClaimed").asBoolean()).isFalse();
    }
}