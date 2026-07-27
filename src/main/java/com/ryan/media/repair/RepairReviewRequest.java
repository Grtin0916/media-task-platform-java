package com.ryan.media.repair;

public record RepairReviewRequest(
        String preference,
        String reason,
        Double confidence,
        String audibleArtifact,
        String forbiddenEventStatus,
        String reviewedBy,
        RepairDecision targetDecision,
        Long reviewVersion) {
}
