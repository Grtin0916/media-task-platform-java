package com.ryan.media.week12;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week12AudioTimingRuntimeContractIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesCloudConsumedAudioTimingRuntimeContract() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/week12/audio-timing-runtime",
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("status").asText()).isEqualTo("PASS");
        assertThat(body.path("source").path("mainbaseCommit").asText()).isEqualTo("28e79ff");

        JsonNode metrics = body.path("metrics");
        assertThat(metrics.path("candidateCount").asInt()).isEqualTo(10);
        assertThat(metrics.path("timingBoundCount").asInt()).isEqualTo(10);
        assertThat(metrics.path("alignmentPassCount").asInt()).isEqualTo(10);
        assertThat(metrics.path("alignmentFailCount").asInt()).isEqualTo(0);
        assertThat(metrics.path("assetTimeModeCounts").path("full_clip").asInt()).isEqualTo(5);
        assertThat(metrics.path("assetTimeModeCounts").path("event_local").asInt()).isEqualTo(5);

        JsonNode offsets = body.path("eventLocalPlacementOffsets");
        assertThat(offsets.isArray()).isTrue();
        assertThat(offsets).hasSize(5);

        for (JsonNode offset : offsets) {
            assertThat(offset.path("placementRequired").asBoolean()).isTrue();
            assertThat(offset.path("expectedStartSec").asText()).isNotBlank();
            assertThat(offset.path("expectedEndSec").asText()).isNotBlank();
            assertThat(offset.path("peakGlobalSec").asText()).isNotBlank();
        }

        assertThat(body.path("runtimeWarnings").toString())
            .contains("EVENT_LOCAL_ASSETS_REQUIRE_EXPECTED_START_SEC_PLACEMENT");
    }

    @Test
    void exposesPlacementRequiredSemanticsForEventLocalAssets() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/week12/audio-timing-runtime/placement-required",
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("assetTimeMode").asText()).isEqualTo("event_local");
        assertThat(body.path("requiredForMixer").asBoolean()).isTrue();
        assertThat(body.path("requiredForDashboard").asBoolean()).isTrue();
        assertThat(body.path("placement").asText()).contains("expectedStartSec");
    }

    @Test
    void exposesExactlyFiveEventLocalPlacementOffsets() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/week12/audio-timing-runtime/event-local-offsets",
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(5);

        assertThat(body.get(0).path("candidateId").asText()).startsWith("procedural_v0_");
        assertThat(body.get(0).path("eventId").asText()).isEqualTo("evt_002");
        assertThat(body.get(0).path("placementRequired").asBoolean()).isTrue();
    }
}
