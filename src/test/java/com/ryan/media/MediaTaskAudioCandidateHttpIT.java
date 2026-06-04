package com.ryan.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaTaskAudioCandidateHttpIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposesAudioCandidateQueueThroughRealHttpStack() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/media-tasks/week12-demo-task/audio-candidates",
                String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotBlank();

        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.path("taskId").asText()).isEqualTo("week12-demo-task");
        assertThat(body.path("source").asText()).isEqualTo("mainbase.week12.enriched_audio_audition_review_queue_v0");
        assertThat(body.path("schemaVersion").asText()).isEqualTo("week12.audio_audition_review_queue.v0");
        assertThat(body.path("status").asText()).isEqualTo("PASS");
        assertThat(body.path("qualityGateStatus").asText()).isEqualTo("HUMAN_AUDITION_REQUIRED");

        assertThat(body.path("candidateCount").asInt()).isEqualTo(10);
        assertThat(body.path("audioProbeOkCount").asInt()).isEqualTo(10);
        assertThat(body.path("audioProbeFailedCount").asInt()).isEqualTo(0);
        assertThat(body.path("durationMissingCount").asInt()).isEqualTo(0);
        assertThat(body.path("sampleRateMissingCount").asInt()).isEqualTo(0);
        assertThat(body.path("eventIdMissingCount").asInt()).isEqualTo(0);
        assertThat(body.path("formatFailedCount").asInt()).isEqualTo(0);

        assertThat(body.path("semanticFidelityClaimedAny").asBoolean()).isFalse();
        assertThat(body.path("mixReadyClaimedAny").asBoolean()).isFalse();

        JsonNode candidates = body.path("candidates");
        assertThat(candidates.isArray()).isTrue();
        assertThat(candidates).hasSize(10);

        JsonNode first = candidates.get(0);
        assertThat(first.path("candidateId").asText()).isEqualTo("procedural_v0_0001");
        assertThat(first.path("caseId").asText()).isEqualTo("seed_0001_case_48b11c");
        assertThat(first.path("sceneId").asText()).isEqualTo("seed_0001_case_48b11c");
        assertThat(first.path("eventId").asText()).isEqualTo("evt_001");
        assertThat(first.path("eventLabel").asText()).isEqualTo("contextual ambience bed");
        assertThat(first.path("layer").asText()).isEqualTo("ambience");
        assertThat(first.path("candidateUri").asText()).contains("artifacts/audio_candidates/");
        assertThat(first.path("durationSec").asDouble()).isGreaterThan(0.0);
        assertThat(first.path("sampleRateHz").asInt()).isEqualTo(16000);
        assertThat(first.path("channels").asInt()).isEqualTo(1);
        assertThat(first.path("sampleWidthBytes").asInt()).isEqualTo(2);
        assertThat(first.path("rmsDbfs").asDouble()).isLessThan(0.0);
        assertThat(first.path("peakDbfs").asDouble()).isLessThan(0.0);
        assertThat(first.path("formatOk").asBoolean()).isTrue();
        assertThat(first.path("reviewStatus").asText()).isEqualTo("HUMAN_AUDITION_REQUIRED");
        assertThat(first.path("failureTags").asText()).contains("human_audition_required");
        assertThat(first.path("failureTags").asText()).contains("semantic_unverified");
        assertThat(first.path("failureTags").asText()).contains("expected_timing_unverified");
        assertThat(first.path("failureTags").asText()).doesNotContain("duration_missing");

        Path runtimeDir = Path.of("artifacts/runtime");
        Files.createDirectories(runtimeDir);

        Path bodyPath = runtimeDir.resolve("week12_audio_candidate_api_http_it_body.json");
        Files.writeString(bodyPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body) + "\n");

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "PASS");
        summary.put("httpCode", response.getStatusCode().value());
        summary.put("endpoint", "/api/media-tasks/week12-demo-task/audio-candidates");
        summary.put("verificationMode", "SpringBootTest.RANDOM_PORT");
        summary.put("candidateCount", body.path("candidateCount").asInt());
        summary.put("audioProbeOkCount", body.path("audioProbeOkCount").asInt());
        summary.put("semanticFidelityClaimedAny", body.path("semanticFidelityClaimedAny").asBoolean());
        summary.put("mixReadyClaimedAny", body.path("mixReadyClaimedAny").asBoolean());
        summary.put("bodyJson", bodyPath.toString());
        summary.put("firstCandidateId", first.path("candidateId").asText());
        summary.put("firstCandidateEventId", first.path("eventId").asText());
        summary.put("firstCandidateDurationSec", first.path("durationSec").asDouble());

        Path summaryPath = runtimeDir.resolve("week12_audio_candidate_api_http_it_summary.json");
        Files.writeString(summaryPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary) + "\n");
    }
}