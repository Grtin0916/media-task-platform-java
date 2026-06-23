package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.stream.Collectors;
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
class Week16TemporalAlignmentRerunPlanIT {

  private static final String ENDPOINT = "/api/week16/temporal-alignment/rerun-plan";

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void returnsWeek16RerunPlanFromEvidenceDrivenFailureRegressionReport() {
    ResponseEntity<JsonNode> response = restTemplate.getForEntity(ENDPOINT, JsonNode.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode body = response.getBody();
    assertThat(body).isNotNull();

    assertThat(body.path("schemaVersion").asText())
        .isEqualTo("week16.java.temporal_alignment.rerun_plan.v1");
    assertThat(body.path("decision").asText()).isEqualTo("PASS_WEEK16_JAVA_RERUN_PLAN");
    assertThat(body.path("sourceMainbaseDecision").asText())
        .isEqualTo("PASS_WEEK16_FAILURE_REGRESSION_REPORT_V0_EVIDENCE_DRIVEN");
    assertThat(body.path("sourceClassificationMode").asText())
        .isEqualTo("evidence_driven_with_expected_fixture_consistency_guard");

    assertThat(body.path("candidateTotal").asInt()).isEqualTo(10);
    assertThat(body.path("eligibleForRerunTotal").asInt()).isEqualTo(2);
    assertThat(body.path("previousAttemptPreservedTotal").asInt()).isEqualTo(10);
    assertThat(body.path("realWorkerTriggeredTotal").asInt()).isEqualTo(0);

    assertThat(body.path("blockedClaims")).hasSizeGreaterThanOrEqualTo(6);

    Map<String, JsonNode> recordsByCandidateId =
        StreamSupport.stream(body.path("records").spliterator(), false)
            .collect(Collectors.toMap(node -> node.path("candidateId").asText(), node -> node));

    assertThat(recordsByCandidateId).hasSize(10);
    assertThat(recordsByCandidateId).containsKeys(
        "procedural_v0_0004", "procedural_v0_0007", "procedural_v0_0010");

    JsonNode candidate0004 = recordsByCandidateId.get("procedural_v0_0004");
    assertThat(candidate0004.path("failureBucket").asText())
        .isEqualTo("P1_ACTIONABLE_REMEDIATED_TIMING_DRIFT");
    assertThat(candidate0004.path("severity").asText()).isEqualTo("P1");
    assertThat(candidate0004.path("fixtureRole").asText()).isEqualTo("paired_regression_fixture");
    assertThat(candidate0004.path("eligibleForRerun").asBoolean()).isTrue();
    assertThat(candidate0004.path("previousAttemptPreserved").asBoolean()).isTrue();
    assertThat(candidate0004.path("realWorkerTriggered").asBoolean()).isFalse();
    assertThat(candidate0004.path("idempotencyKeyPolicy").asText()).contains("candidateId");

    JsonNode candidate0010 = recordsByCandidateId.get("procedural_v0_0010");
    assertThat(candidate0010.path("failureBucket").asText())
        .isEqualTo("P1_ACTIONABLE_REMEDIATED_TIMING_DRIFT");
    assertThat(candidate0010.path("severity").asText()).isEqualTo("P1");
    assertThat(candidate0010.path("fixtureRole").asText()).isEqualTo("paired_regression_fixture");
    assertThat(candidate0010.path("eligibleForRerun").asBoolean()).isTrue();
    assertThat(candidate0010.path("previousAttemptPreserved").asBoolean()).isTrue();
    assertThat(candidate0010.path("realWorkerTriggered").asBoolean()).isFalse();

    JsonNode candidate0007 = recordsByCandidateId.get("procedural_v0_0007");
    assertThat(candidate0007.path("failureBucket").asText())
        .isEqualTo("P2_WARN_NEAR_MISS_THRESHOLD_MARGIN");
    assertThat(candidate0007.path("severity").asText()).isEqualTo("P2");
    assertThat(candidate0007.path("fixtureRole").asText()).isEqualTo("threshold_margin_fixture");
    assertThat(candidate0007.path("eligibleForRerun").asBoolean()).isFalse();
    assertThat(candidate0007.path("previousAttemptPreserved").asBoolean()).isTrue();
    assertThat(candidate0007.path("realWorkerTriggered").asBoolean()).isFalse();
  }

  @TestConfiguration
  static class Week16RerunPlanTestSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain week16RerunPlanTestChain(HttpSecurity http) throws Exception {
      return http
          .securityMatcher(ENDPOINT)
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }
  }
}