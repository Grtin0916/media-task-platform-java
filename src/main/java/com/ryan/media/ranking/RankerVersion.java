package com.ryan.media.ranking;

import java.time.Instant;

public record RankerVersion(
        String rankerVersion,
        String bundleDigest,
        RankerPromotionStatus promotionStatus,
        boolean modelPresent,
        boolean oofAvailable,
        int recommendationCount,
        String featureSchemaVersion,
        String featureSnapshotDigest,
        int reviewSubmittedCount,
        boolean humanReviewCompleted,
        int finalSelectedMutationCount,
        String blockedReason,
        String storedBundlePath,
        Instant importedAt) {
}
