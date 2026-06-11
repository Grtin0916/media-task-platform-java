package com.ryan.media.week13;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week13CandidateBankDemoReadinessControllerIT {

    @LocalServerPort
    int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void shouldExposeCandidateBankDemoReadinessOverHttp() {
        String url = "http://localhost:" + port + "/api/week13/candidate-bank-demo-readiness";

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        Map<?, ?> body = response.getBody();
        assertThat(body.get("status")).isEqualTo("PASS");
        assertThat(body.get("scope")).isEqualTo("java-platform-http-readiness-api");
        assertThat(body.get("sourceScope")).isEqualTo("java-platform-consumption-report-only");

        Map<?, ?> counts = (Map<?, ?>) body.get("consumedCounts");
        assertThat(numberValue(counts, "candidateCount")).isEqualTo(10);
        assertThat(numberValue(counts, "placementTableRows")).isEqualTo(10);
        assertThat(numberValue(counts, "materializedCount")).isEqualTo(10);
        assertThat(numberValue(counts, "mountReadableCount")).isEqualTo(10);
        assertThat(numberValue(counts, "workerReadyCount")).isEqualTo(10);
        assertThat(numberValue(counts, "workerSuccessCount")).isEqualTo(10);

        Map<?, ?> javaStatuses = (Map<?, ?>) body.get("javaStatuses");
        assertThat(javaStatuses.get("javaRegistryContract")).isEqualTo("PASS");
        assertThat(javaStatuses.get("javaMaterializedReadiness")).isEqualTo("PASS");
        assertThat(javaStatuses.get("javaReadinessApiContract")).isEqualTo("PASS");

        List<?> blockers = (List<?>) body.get("blockers");
        assertThat(blockers).isEmpty();

        List<?> boundary = (List<?>) body.get("boundary");
        assertThat(boundary.stream().map(Object::toString).toList()).contains(
                "does_not_claim_semantic_audio_quality",
                "does_not_claim_human_audition_pass",
                "does_not_claim_final_mix_readiness",
                "does_not_claim_production_kubernetes_job",
                "does_not_claim_s3_minio_csi_or_cloud_object_storage",
                "does_not_claim_durable_java_registry"
        );
    }

    private static int numberValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).intValue();
    }
}