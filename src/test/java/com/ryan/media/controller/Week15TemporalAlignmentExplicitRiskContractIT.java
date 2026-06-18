package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.ryan.media.security.K6SmokeSecurityTestConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "week15.explicitRiskContract.path=artifacts/manifests/week15_mainbase_explicit_risk_contract.json"
)
@Import(K6SmokeSecurityTestConfig.class)
class Week15TemporalAlignmentExplicitRiskContractIT {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void exposesExplicitRiskContractFromMainbaseSnapshot() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/week15/temporal-alignment/explicit-risk-contract",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("decision").asText()).isEqualTo("PASS");
        assertThat(body.path("gateDecision").asText()).isEqualTo("PASS_EXPLICIT_RISK_CONTRACT_CONSUMER");
        assertThat(body.path("candidateTotal").asInt()).isEqualTo(10);
        assertThat(strings(body.path("actionableRiskCandidateIds"))).containsExactly(
                "procedural_v0_0004",
                "procedural_v0_0010"
        );
        assertThat(strings(body.path("alertEligibleCandidateIds"))).containsExactly(
                "procedural_v0_0004",
                "procedural_v0_0010"
        );
        assertThat(strings(body.path("nonActionableCandidateIds"))).contains(
                "procedural_v0_0002",
                "procedural_v0_0003",
                "procedural_v0_0007"
        );
        assertThat(body.path("candidateRisks")).hasSize(10);
    }

    private static List<String> strings(JsonNode node) {
        List<String> out = new ArrayList<>();
        for (JsonNode item : node) {
            out.add(item.asText());
        }
        return out;
    }
}
