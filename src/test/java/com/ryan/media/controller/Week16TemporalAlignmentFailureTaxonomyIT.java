package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week16TemporalAlignmentFailureTaxonomyIT {

    private static final String ENDPOINT = "/api/week16/temporal-alignment/failure-taxonomy";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returnsWeek16FailureTaxonomyPayloadOverHttp() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(ENDPOINT, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("schemaVersion").asText())
                .isEqualTo("week16.java.temporal_alignment.failure_taxonomy.payload.v1");
        assertThat(body.path("sourceMode").asText())
                .isEqualTo("mainbase_week16_failure_taxonomy_seed");
        assertThat(body.path("apiCandidate").asText()).isEqualTo(ENDPOINT);
        assertThat(body.path("candidateTotal").asInt()).isEqualTo(10);

        JsonNode bucketCounts = body.path("bucketCounts");
        assertThat(bucketCounts.path("pass_low_risk_with_numeric_margin").asInt()).isEqualTo(7);
        assertThat(bucketCounts.path("timing_drift_actionable_remediated").asInt()).isEqualTo(2);
        assertThat(bucketCounts.path("warn_near_miss_threshold_margin").asInt()).isEqualTo(1);

        List<String> regressionIds = StreamSupport
                .stream(body.path("regressionFixtures").spliterator(), false)
                .map(node -> node.path("candidateId").asText())
                .toList();
        assertThat(regressionIds)
                .containsExactly("procedural_v0_0004", "procedural_v0_0010");

        body.path("regressionFixtures").forEach(node -> {
            assertThat(node.path("hasWaveformEvidence").asBoolean()).isTrue();
            assertThat(node.path("originalAbsOnsetDeltaSec").asDouble()).isGreaterThan(1.0d);
            assertThat(node.path("remediatedAbsOnsetDeltaSec").asDouble()).isLessThan(0.05d);
        });

        List<String> thresholdIds = StreamSupport
                .stream(body.path("thresholdFixtures").spliterator(), false)
                .map(node -> node.path("candidateId").asText())
                .toList();
        assertThat(thresholdIds).containsExactly("procedural_v0_0007");

        assertThat(body.path("passControlFixtures")).hasSize(7);
        assertThat(body.path("blockedClaims")).hasSize(4);
    }

    @TestConfiguration
    static class Week16FailureTaxonomyTestSecurityConfig {

        @Bean
        @Order(0)
        SecurityFilterChain week16FailureTaxonomyTestChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher(ENDPOINT)
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
}
