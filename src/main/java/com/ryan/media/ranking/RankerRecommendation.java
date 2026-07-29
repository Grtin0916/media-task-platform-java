package com.ryan.media.ranking;

import java.time.Instant;

public record RankerRecommendation(
        String recommendationId,
        String caseId,
        String rankerVersion,
        String bundleDigest,
        String featureSnapshotDigest,
        String candidate,
        Double score,
        Double margin,
        RankerRecommendationStatus recommendationStatus,
        String reason,
        Instant createdAt,
        String supersedesRecommendationId) {
}
