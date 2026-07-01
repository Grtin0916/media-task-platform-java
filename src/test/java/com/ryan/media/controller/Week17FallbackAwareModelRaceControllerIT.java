package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17FallbackAwareModelRaceControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesFallbackAwareModelRaceResultFromMainbasePayload() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/week17/fallback-aware-model-race/result",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("artifact_type")).isEqualTo("week17_fallback_aware_model_race_result");
        assertThat(body.get("true_mmaudio_status")).isEqualTo("blocked_by_torch_torchaudio_abi");
        assertThat(((Number) body.get("case_count")).intValue()).isEqualTo(6);
        assertThat(((Number) body.get("winner_count")).intValue()).isEqualTo(6);
        assertThat(((Number) body.get("canonical_candidate_count")).intValue()).isEqualTo(28);
        assertThat(body.get("java_consumer_status")).isEqualTo("consumed");

        Object items = body.get("items");
        assertThat(items).isInstanceOf(List.class);
        assertThat((List<?>) items).hasSize(6);
    }
}
