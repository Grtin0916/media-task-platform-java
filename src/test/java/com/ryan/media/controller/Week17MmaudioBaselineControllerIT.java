package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Week17MmaudioBaselineControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void summaryExposesHonestFallbackBoundary() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/mmaudio-baseline/summary",
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("PASS_JAVA_MMAUDIO_BASELINE_API_READY");
        assertThat(body.get("caseCount")).isEqualTo(6);
        assertThat(body.get("candidateCount")).isEqualTo(12);
        assertThat(body.get("winnerCount")).isEqualTo(6);
        assertThat(body.get("repairQueueCount")).isEqualTo(6);
        assertThat(body.get("badPromptCount")).isEqualTo(0);
        assertThat(body.get("trueMmaudioGeneratedCount")).isEqualTo(0);
        assertThat(body.get("allOutputsAreFallback")).isEqualTo(true);
        assertThat(body.get("canClaimTrueMmaudioV2aSuccess")).isEqualTo(false);
        assertThat(body.get("canClaimVideoSynchronizedQuality")).isEqualTo(false);
    }

    @Test
    void winnersAndRepairQueueAreReadable() {
        ResponseEntity<JsonNode> winners = restTemplate.getForEntity(
                "/api/week17/mmaudio-baseline/winners",
                JsonNode.class
        );
        ResponseEntity<JsonNode> repairQueue = restTemplate.getForEntity(
                "/api/week17/mmaudio-baseline/repair-queue",
                JsonNode.class
        );

        assertThat(winners.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(repairQueue.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(winners.getBody()).isNotNull();
        assertThat(repairQueue.getBody()).isNotNull();
        assertThat(winners.getBody().isArray()).isTrue();
        assertThat(repairQueue.getBody().isArray()).isTrue();
        assertThat(winners.getBody().size()).isEqualTo(6);
        assertThat(repairQueue.getBody().size()).isEqualTo(6);
    }

    @Test
    void healthSeesCopiedPayload() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/mmaudio-baseline/health",
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("payloadExists")).isEqualTo(true);
    }
}
