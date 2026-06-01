package com.ryan.media.model;

/**
 * Week12 API-facing view for exposing the task -> input asset -> Blueprint V1 binding.
 *
 * Boundary:
 * - This is a response/view contract, not a controller endpoint.
 * - It does not claim persistence, signed URL lifecycle, or object-store retention.
 * - It is intended to be wired into MediaTaskResponse or an endpoint after the JSON shape is pinned.
 */
public record MediaTaskAssetBlueprintView(
        String taskId,
        String inputAssetUri,
        String blueprintArtifactUri,
        String blueprintId,
        String timelineArtifactUri,
        String qualityGateStatus,
        boolean qualityGatePassed,
        boolean blueprintManifestLinked,
        boolean timelineArtifactLinked
) {
    public static MediaTaskAssetBlueprintView fromBinding(MediaTaskAssetBlueprintBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding must not be null");
        }

        return new MediaTaskAssetBlueprintView(
                binding.taskId(),
                binding.inputAssetUri(),
                binding.blueprintArtifactUri(),
                binding.blueprintId(),
                binding.timelineArtifactUri(),
                binding.qualityGateStatus(),
                binding.isQualityGatePassed(),
                binding.pointsToBlueprintManifest(),
                binding.pointsToTimelineArtifact()
        );
    }

    public boolean readyForApiExposure() {
        return blueprintManifestLinked && timelineArtifactLinked;
    }

    public boolean readyForSucceededTaskExposure() {
        return readyForApiExposure() && qualityGatePassed;
    }
}