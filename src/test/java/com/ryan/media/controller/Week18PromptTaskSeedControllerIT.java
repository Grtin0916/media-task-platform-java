package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week18PromptTaskSeedControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesWeek18PromptTaskSeed() {
        String url = "http://localhost:" + port + "/api/week18/prompt-task-seed";

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("contractVersion").asText())
                .isEqualTo("week18-prompt-task-seed-v1");
        assertThat(body.path("promptTaskSeedReady").asBoolean()).isTrue();

        JsonNode promptTasks = body.path("promptTasks");
        assertThat(promptTasks.path("taskCount").asInt()).isEqualTo(12);
        assertThat(promptTasks.path("caseCount").asInt()).isEqualTo(6);
        assertThat(promptTasks.path("promptTypeCounts").path("naive").asInt()).isEqualTo(6);
        assertThat(promptTasks.path("promptTypeCounts").path("dss").asInt()).isEqualTo(6);
        assertThat(promptTasks.path("allCasesHaveNaiveAndDss").asBoolean()).isTrue();
        assertThat(promptTasks.path("trueAnchorTaskCount").asInt()).isEqualTo(2);
        assertThat(promptTasks.path("repairTargetCount").asInt()).isGreaterThanOrEqualTo(6);

        JsonNode boundary = body.path("claimBoundary");
        assertThat(boundary.path("boundaryPreserved").asBoolean()).isTrue();
        assertThat(boundary.path("trueMmaudioBatchSuccess").asBoolean()).isFalse();
        assertThat(boundary.path("fullCandidateRankingAvailable").asBoolean()).isFalse();
        assertThat(boundary.path("productionSloVerified").asBoolean()).isFalse();
        assertThat(boundary.path("k6ThresholdPassVerified").asBoolean()).isFalse();
        assertThat(boundary.path("liveGrafanaImportVerified").asBoolean()).isFalse();

        JsonNode javaApi = body.path("javaApi");
        assertThat(javaApi.path("endpoint").asText()).isEqualTo("/api/week18/prompt-task-seed");
        assertThat(javaApi.path("randomPortITOnly").asBoolean()).isTrue();
        assertThat(javaApi.path("liveServiceAvailabilityClaimed").asBoolean()).isFalse();
    }
}