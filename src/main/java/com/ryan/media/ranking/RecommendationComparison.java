package com.ryan.media.ranking;

public record RecommendationComparison(
        String caseId,
        String fromVersion,
        String toVersion,
        RankerPromotionStatus fromPromotionStatus,
        RankerPromotionStatus toPromotionStatus,
        RankerRecommendationStatus fromRecommendationStatus,
        RankerRecommendationStatus toRecommendationStatus,
        boolean changed) {
}
