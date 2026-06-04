
package com.ryan.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryan.media.api.MediaTaskAudioCandidateController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MediaTaskAudioCandidateControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void exposesMainbaseEnrichedAudioCandidateReviewQueue() throws Exception {
        MediaTaskAudioCandidateController controller =
                new MediaTaskAudioCandidateController(new ObjectMapper());

        ResponseEntity<Map<String, Object>> response =
                controller.getAudioCandidates("week12-demo-task");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("taskId", "week12-demo-task");
        assertThat(body).containsEntry("status", "PASS");
        assertThat(body).containsEntry("qualityGateStatus", "HUMAN_AUDITION_REQUIRED");
        assertThat(body).containsEntry("candidateCount", 10);
        assertThat(body).containsEntry("audioProbeOkCount", 10);
        assertThat(body).containsEntry("audioProbeFailedCount", 0);
        assertThat(body).containsEntry("durationMissingCount", 0);
        assertThat(body).containsEntry("sampleRateMissingCount", 0);
        assertThat(body).containsEntry("eventIdMissingCount", 0);
        assertThat(body).containsEntry("semanticFidelityClaimedAny", false);
        assertThat(body).containsEntry("mixReadyClaimedAny", false);

        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) body.get("candidates");

        assertThat(candidates).hasSize(10);

        Map<String, Object> first = candidates.get(0);
        assertThat(first).containsEntry("candidateId", "procedural_v0_0001");
        assertThat(first).containsEntry("caseId", "seed_0001_case_48b11c");
        assertThat(first).containsEntry("eventId", "evt_001");
        assertThat(first).containsEntry("layer", "ambience");
        assertThat(first).containsEntry("durationSec", 8.0);
        assertThat(first).containsEntry("sampleRateHz", 16000);
        assertThat(first).containsEntry("channels", 1);
        assertThat(first).containsEntry("sampleWidthBytes", 2);
        assertThat(first).containsEntry("formatOk", true);
        assertThat(first).containsEntry("reviewStatus", "HUMAN_AUDITION_REQUIRED");

        String failureTags = String.valueOf(first.get("failureTags"));
        assertThat(failureTags).contains("human_audition_required");
        assertThat(failureTags).contains("semantic_unverified");
        assertThat(failureTags).contains("expected_timing_unverified");
        assertThat(failureTags).doesNotContain("duration_missing");
    }
}
