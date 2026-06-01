package com.ryan.media.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Week12 V1 binding between a media task, its input asset, and a Mainbase
 * SoundLayer Blueprint artifact.
 *
 * Boundary:
 * - This is a local/domain contract, not an object-store registry.
 * - It does not claim signed URL lifecycle, durable queue semantics, or exactly-once processing.
 * - It is designed to let API/service layers expose which task consumes which Blueprint V1 artifact.
 */
public record MediaTaskAssetBlueprintBinding(
        String taskId,
        String inputAssetUri,
        String blueprintArtifactUri,
        String blueprintId,
        String timelineArtifactUri,
        String qualityGateStatus
) {
    public MediaTaskAssetBlueprintBinding {
        taskId = requireNonBlank(taskId, "taskId");
        inputAssetUri = requireNonBlank(inputAssetUri, "inputAssetUri");
        blueprintArtifactUri = requireNonBlank(blueprintArtifactUri, "blueprintArtifactUri");
        blueprintId = requireNonBlank(blueprintId, "blueprintId");
        timelineArtifactUri = requireNonBlank(timelineArtifactUri, "timelineArtifactUri");
        qualityGateStatus = normalizeQualityGateStatus(qualityGateStatus);
    }

    public boolean isQualityGatePassed() {
        return "PASS".equals(qualityGateStatus);
    }

    public boolean pointsToBlueprintManifest() {
        return blueprintArtifactUri.contains("week12_blueprint_v1_manifest.json")
                && blueprintArtifactUri.contains("#")
                && blueprintArtifactUri.contains(blueprintId);
    }

    public boolean pointsToTimelineArtifact() {
        return timelineArtifactUri.endsWith(".csv") || timelineArtifactUri.endsWith(".jsonl");
    }

    public static MediaTaskAssetBlueprintBinding week12BlueprintV1(
            String taskId,
            String inputAssetUri,
            String blueprintId,
            String qualityGateStatus
    ) {
        String manifestUri = "artifacts/manifests/week12_blueprint_v1_manifest.json#" + requireNonBlank(blueprintId, "blueprintId");
        String timelineUri = "artifacts/manifests/week12_event_timeline.jsonl";
        return new MediaTaskAssetBlueprintBinding(
                taskId,
                inputAssetUri,
                manifestUri,
                blueprintId,
                timelineUri,
                qualityGateStatus
        );
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeQualityGateStatus(String value) {
        String normalized = requireNonBlank(value, "qualityGateStatus").trim().toUpperCase(Locale.ROOT);
        if (!Objects.equals(normalized, "PASS")
                && !Objects.equals(normalized, "WARN")
                && !Objects.equals(normalized, "FAIL")) {
            throw new IllegalArgumentException("qualityGateStatus must be one of PASS, WARN, FAIL");
        }
        return normalized;
    }
}