package com.ryan.media.week13;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week13MaterializedAudioRegistryControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldExposeMaterializedAudioRegistryReadiness() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/week13/audio-artifact-registry/materialized-readiness",
            Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("PASS");
        assertThat(body.get("candidateCount")).isEqualTo(10);
        assertThat(body.get("readyCount")).isEqualTo(10);
        assertThat(body.get("missingInJavaCount")).isEqualTo(0);
        assertThat(body.get("missingInCloudCount")).isEqualTo(0);
        assertThat(body.get("blockers")).isEqualTo(List.of());
        assertThat(body.get("apiScope")).isEqualTo("week13_java_materialized_readiness_api_v1");

        Map<String, Object> modeCounts = (Map<String, Object>) body.get("assetTimeModeCounts");
        assertThat(modeCounts).containsEntry("full_clip", 5);
        assertThat(modeCounts).containsEntry("event_local", 5);

        List<Map<String, Object>> records = (List<Map<String, Object>>) body.get("records");
        assertThat(records).hasSize(10);
        assertThat(records)
            .allSatisfy(record -> {
                assertThat(record.get("materializedStorageStatus")).isEqualTo("READY");
                assertThat((String) record.get("podPath")).startsWith("/mnt/audio-candidates/week13/");
                assertThat(record.get("audioReadable")).isEqualTo(true);
                assertThat(record.get("sampleRateHz")).isEqualTo(16000);
                assertThat(record.get("channels")).isEqualTo(1);
                assertThat(record.get("durationSec")).isNotNull();
            });
    }
}
