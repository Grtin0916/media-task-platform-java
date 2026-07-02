package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17TrueAwareResultCardControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void summaryShouldExposeClaimSafeTrueAwareResultCard() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/true-aware/result-card/summary", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("status")).isEqualTo("consumer_ready");
        assertThat(body.get("claimLevel")).isEqualTo("single_true_v2a_candidate_available");
        assertThat(body.get("primaryCaseId")).isEqualTo("glass_drop_room_001");
        assertThat(body.get("primaryModel")).isEqualTo("MMAudio");
        assertThat(body.get("safeTrueMmaudioRecordCount")).isEqualTo(1);
        assertThat(body.get("rawCandidateRecordCount")).isEqualTo(9);
        assertThat(body.get("trueMmaudioSingleSuccess")).isEqualTo(true);
        assertThat(body.get("trueMmaudioBatchSuccess")).isEqualTo(false);
        assertThat(body.get("fullCandidateRankingAvailable")).isEqualTo(false);
        assertThat(body.get("productionSloVerified")).isEqualTo(false);
        assertThat(body.get("k6ThresholdPassVerified")).isEqualTo(false);
    }

    @Test
    void healthShouldConfirmArtifactBackedContract() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/true-aware/result-card/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("status")).isEqualTo("UP");
        assertThat(body.get("artifactBacked")).isEqualTo(true);
        assertThat(body.get("bridgeExists")).isEqualTo(true);
        assertThat(body.get("claimGuardExists")).isEqualTo(true);
        assertThat(body.get("claimLevel")).isEqualTo("single_true_v2a_candidate_available");
    }
}
