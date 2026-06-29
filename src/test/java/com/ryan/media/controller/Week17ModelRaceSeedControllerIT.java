package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17ModelRaceSeedControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesMainbaseModelRaceSeedPayload() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            url("/api/week17/model-race-seed"),
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = Objects.requireNonNull(response.getBody());

        assertThat(body.path("schema_version").asText()).isEqualTo("model_race_seed.v0.1");
        assertThat(body.path("source").asText()).isEqualTo("mainbase");
        assertThat(body.path("decision").asText()).isEqualTo("PASS_WITH_REPAIR_QUEUE");
        assertThat(body.path("results").size()).isEqualTo(6);
    }

    @Test
    void exposesMainbaseRepairSeedPayload() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            url("/api/week17/repair-seed"),
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = Objects.requireNonNull(response.getBody());

        assertThat(body.path("schema_version").asText()).isEqualTo("repair_seed.v0.1");
        assertThat(body.path("source").asText()).isEqualTo("mainbase");
        assertThat(body.path("decision").asText()).isEqualTo("PASS_REPAIR_CLOSED_QUEUE");
        assertThat(body.path("results").size()).isEqualTo(1);
        assertThat(body.path("results").get(0).path("winner_after_repair").asBoolean()).isTrue();
    }

    @Test
    void exposesJavaPayloadIngestionSummary() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            url("/api/week17/model-race-seed/summary"),
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = Objects.requireNonNull(response.getBody());

        assertThat(body.path("source").asText()).isEqualTo("mainbase");
        assertThat(body.path("model_race_result_count").asInt()).isEqualTo(6);
        assertThat(body.path("repair_result_count").asInt()).isEqualTo(1);
        assertThat(body.path("repair_closed").asBoolean()).isTrue();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}