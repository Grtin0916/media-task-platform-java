package com.ryan.media.week18;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
    classes = W18EvaluationHandoffLiveApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class W18EvaluationHandoffControllerIT {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesArtifactBackedEvaluationSummary() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/week18/evaluation/summary", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("week")).isEqualTo("week18");
        assertThat(response.getBody().get("phase")).isEqualTo("dss_vs_naive_evaluation");
        assertThat(response.getBody()).containsKeys("closure", "audioMetrics", "pairwise", "selector", "repairSeed");
    }
}
