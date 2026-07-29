package com.ryan.media.ranking;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record RankerBundleManifest(
        String schemaVersion,
        String rankerName,
        String rankerVersion,
        RankerPromotionStatus promotionStatus,
        String bundleDigest,
        boolean modelPresent,
        boolean oofAvailable,
        int recommendationCount,
        String featureSchemaVersion,
        String featureSnapshotDigest,
        String trainingDatasetDigest,
        String trainingCodeCommit,
        String sourceGitHead,
        int reviewSubmittedCount,
        boolean humanReviewCompleted,
        int finalSelectedMutationCount,
        String blockedReason,
        String activeLearningStatus,
        List<RankerArtifactRef> artifacts,
        JsonNode claimBoundary) {
}
