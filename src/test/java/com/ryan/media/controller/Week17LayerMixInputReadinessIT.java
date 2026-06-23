package com.ryan.media.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week17LayerMixInputReadinessIT {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesReadOnlyLayerMixInputReadinessContractWithoutTriggeringMixer() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/week16/temporal-alignment/layer-mix-input-readiness",
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("decision").asText())
                .isEqualTo("READ_ONLY_LAYER_MIX_INPUT_READINESS_CONTRACT");
        assertThat(body.path("artifactContractVersion").asText())
                .isEqualTo("week17.layer_mix_input_readiness.v0");
        assertThat(body.path("realMixerTriggered").asBoolean(true)).isFalse();
        assertThat(body.path("realWorkerTriggered").asBoolean(true)).isFalse();
        assertThat(body.path("previousS3EvidencePreserved").asBoolean(false)).isTrue();
        assertThat(body.path("blockedClaims").isArray()).isTrue();
        assertThat(body.path("consumerReport").isObject()).isTrue();
        assertThat(body.path("mainbasePayload").isObject()).isTrue();
    }
}
