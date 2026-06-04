#!/usr/bin/env python3
"""
Build Week12 Java audio candidate API from Mainbase enriched review queue.

This script:
- imports the Mainbase enriched review queue JSON as a Java classpath resource;
- creates a Spring Boot controller exposing /api/media-tasks/{taskId}/audio-candidates;
- creates a focused controller test;
- writes a small API contract document;
- writes an import summary artifact.

Boundary:
- Java only exposes task-linked candidate metadata.
- Java does not claim semantic audio quality.
- Java does not claim human audition pass.
- Java does not claim mix readiness.
- Java does not implement a durable artifact registry yet.
"""

from __future__ import annotations

import json
import os
import shutil
from datetime import datetime, timezone
from pathlib import Path


JAVA_ROOT = Path(".").resolve()
MAINBASE = Path(os.environ.get(
    "MAINBASE",
    str(Path.home() / "work" / "audio_engineering_repo_skeleton_v1")
)).resolve()

SOURCE_QUEUE = MAINBASE / "artifacts/evals/week12_audio_audition_review_queue_v0.json"
RESOURCE_QUEUE = JAVA_ROOT / "src/main/resources/week12-audio-audition-review-queue-v0.json"

CONTROLLER = JAVA_ROOT / "src/main/java/com/ryan/media/api/MediaTaskAudioCandidateController.java"
TEST = JAVA_ROOT / "src/test/java/com/ryan/media/MediaTaskAudioCandidateControllerTest.java"
DOC = JAVA_ROOT / "docs/api/week12-audio-candidate-contract.md"
SUMMARY = JAVA_ROOT / "artifacts/runtime/week12_audio_candidate_api_import_summary.json"


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat()


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.rstrip() + "\n", encoding="utf-8")


def main() -> int:
    if not SOURCE_QUEUE.exists():
        raise SystemExit(f"ERROR: missing Mainbase review queue: {SOURCE_QUEUE}")

    queue = json.loads(SOURCE_QUEUE.read_text(encoding="utf-8"))

    required = {
        "status": "PASS",
        "candidateCount": 10,
        "audioProbeOkCount": 10,
        "audioProbeFailedCount": 0,
        "durationMissingCount": 0,
        "sampleRateMissingCount": 0,
        "eventIdMissingCount": 0,
        "semanticFidelityClaimedAny": False,
        "mixReadyClaimedAny": False,
    }

    for key, expected in required.items():
        actual = queue.get(key)
        if actual != expected:
            raise SystemExit(f"ERROR: queue field mismatch: {key}: expected={expected!r}, actual={actual!r}")

    review_queue = queue.get("reviewQueue")
    if not isinstance(review_queue, list) or len(review_queue) != 10:
        raise SystemExit("ERROR: reviewQueue must contain 10 candidates")

    first = review_queue[0]
    for key in [
        "candidateId",
        "sourceRequestId",
        "caseId",
        "sceneId",
        "eventId",
        "eventLabel",
        "layer",
        "candidateUri",
        "durationSec",
        "sampleRateHz",
        "channels",
        "sampleWidthBytes",
        "rmsDbfs",
        "peakDbfs",
        "formatOk",
        "reviewStatus",
        "failureTags",
    ]:
        if first.get(key) in (None, ""):
            raise SystemExit(f"ERROR: first candidate missing required field: {key}")

    RESOURCE_QUEUE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(SOURCE_QUEUE, RESOURCE_QUEUE)

    controller_code = r'''
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
'''
    write(CONTROLLER, controller_code)

    test_code = r'''
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
'''
    write(TEST, test_code)

    doc = f'''
# Week12 Audio Candidate API Contract

Generated on: {utc_now()}

## Endpoint

`GET /api/media-tasks/{{taskId}}/audio-candidates`

## Purpose

Expose the Mainbase Week12 enriched audio audition review queue through the Java media-task platform.

This is a task-facing API view over candidate metadata. It does not generate audio and does not evaluate semantic audio quality.

## Source artifact

`mainbase:artifacts/evals/week12_audio_audition_review_queue_v0.json`

Imported Java classpath resource:

`src/main/resources/week12-audio-audition-review-queue-v0.json`

## Verified fields

- `taskId`
- `source`
- `schemaVersion`
- `status`
- `qualityGateStatus`
- `reviewQueueArtifactUri`
- `candidateCount`
- `audioProbeOkCount`
- `durationMissingCount`
- `sampleRateMissingCount`
- `eventIdMissingCount`
- `semanticFidelityClaimedAny`
- `mixReadyClaimedAny`
- `doesNotClaim`
- `candidates`

## Boundary

- This is not a complete artifact registry.
- This is not production object storage.
- This is not durable worker orchestration.
- This does not claim semantic audio quality passed.
- This does not claim human audition passed.
- This does not claim final mix readiness.
'''
    write(DOC, doc)

    summary = {
        "generatedAt": utc_now(),
        "status": "PASS",
        "sourceQueue": str(SOURCE_QUEUE),
        "resourceQueue": str(RESOURCE_QUEUE.relative_to(JAVA_ROOT)),
        "controller": str(CONTROLLER.relative_to(JAVA_ROOT)),
        "test": str(TEST.relative_to(JAVA_ROOT)),
        "doc": str(DOC.relative_to(JAVA_ROOT)),
        "candidateCount": queue.get("candidateCount"),
        "audioProbeOkCount": queue.get("audioProbeOkCount"),
        "semanticFidelityClaimedAny": queue.get("semanticFidelityClaimedAny"),
        "mixReadyClaimedAny": queue.get("mixReadyClaimedAny"),
        "endpoint": "/api/media-tasks/{taskId}/audio-candidates",
        "boundary": [
            "not_complete_artifact_registry",
            "not_production_object_storage",
            "not_human_audition_passed",
            "not_semantic_audio_quality_passed",
            "not_mix_ready",
        ],
    }
    SUMMARY.parent.mkdir(parents=True, exist_ok=True)
    SUMMARY.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())