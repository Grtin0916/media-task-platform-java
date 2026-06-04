
package com.ryan.media.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media-tasks")
public class MediaTaskAudioCandidateController {

    private static final String QUEUE_RESOURCE = "week12-audio-audition-review-queue-v0.json";

    private final ObjectMapper objectMapper;

    public MediaTaskAudioCandidateController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{taskId}/audio-candidates")
    public ResponseEntity<Map<String, Object>> getAudioCandidates(@PathVariable String taskId) throws IOException {
        JsonNode root = loadQueue();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("source", "mainbase.week12.enriched_audio_audition_review_queue_v0");
        body.put("schemaVersion", text(root, "schemaVersion"));
        body.put("status", text(root, "status"));
        body.put("qualityGateStatus", "HUMAN_AUDITION_REQUIRED");
        body.put("reviewQueueArtifactUri", "mainbase:artifacts/evals/week12_audio_audition_review_queue_v0.json");
        body.put("classpathResource", QUEUE_RESOURCE);

        body.put("candidateCount", intValue(root, "candidateCount"));
        body.put("audioProbeOkCount", intValue(root, "audioProbeOkCount"));
        body.put("audioProbeFailedCount", intValue(root, "audioProbeFailedCount"));
        body.put("durationMissingCount", intValue(root, "durationMissingCount"));
        body.put("sampleRateMissingCount", intValue(root, "sampleRateMissingCount"));
        body.put("eventIdMissingCount", intValue(root, "eventIdMissingCount"));
        body.put("formatFailedCount", intValue(root, "formatFailedCount"));

        body.put("allRequireHumanAudition", boolValue(root, "allRequireHumanAudition"));
        body.put("semanticFidelityClaimedAny", boolValue(root, "semanticFidelityClaimedAny"));
        body.put("mixReadyClaimedAny", boolValue(root, "mixReadyClaimedAny"));
        body.put("doesNotClaim", stringList(root.path("doesNotClaim")));
        body.put("blockers", stringList(root.path("blockers")));
        body.put("candidates", candidateList(root.path("reviewQueue")));

        return ResponseEntity.ok(body);
    }

    private JsonNode loadQueue() throws IOException {
        ClassPathResource resource = new ClassPathResource(QUEUE_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    private String text(JsonNode root, String field) {
        return root.path(field).asText("");
    }

    private int intValue(JsonNode root, String field) {
        return root.path(field).asInt(0);
    }

    private boolean boolValue(JsonNode root, String field) {
        return root.path(field).asBoolean(false);
    }

    private List<String> stringList(JsonNode node) {
        return objectMapper.convertValue(node, new TypeReference<List<String>>() {});
    }

    private List<Map<String, Object>> candidateList(JsonNode node) {
        return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
    }
}
