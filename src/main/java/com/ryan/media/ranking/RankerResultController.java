package com.ryan.media.ranking;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranker-results")
public class RankerResultController {
    private final RankerRegistry registry;
    private final RecommendationHistory history;

    public RankerResultController(RankerRegistry registry, RecommendationHistory history) {
        this.registry = registry;
        this.history = history;
    }

    @GetMapping("/{caseId}")
    public ResultView result(@PathVariable String caseId) {
        return view(caseId, registry.latest());
    }

    @GetMapping("/{caseId}/history")
    public List<RankerRecommendation> history(@PathVariable String caseId) {
        return history.forCase(caseId);
    }

    @GetMapping("/{caseId}/compare")
    public RecommendationComparison compare(
            @PathVariable String caseId,
            @RequestParam String from,
            @RequestParam String to) {
        ResultView left = view(caseId, registry.require(from));
        ResultView right = view(caseId, registry.require(to));
        return new RecommendationComparison(
                caseId, from, to,
                left.rankerPromotionStatus(), right.rankerPromotionStatus(),
                left.recommendationStatus(), right.recommendationStatus(),
                left.recommendationStatus() != right.recommendationStatus()
                        || left.rankerPromotionStatus() != right.rankerPromotionStatus());
    }

    private ResultView view(String caseId, RankerVersion version) {
        if (version.promotionStatus() == RankerPromotionStatus.DATA_BLOCKED) {
            return new ResultView(
                    caseId,
                    version.rankerVersion(),
                    version.promotionStatus(),
                    RankerRecommendationStatus.UNAVAILABLE,
                    HumanReviewStatus.NOT_STARTED,
                    "PROVISIONAL_SELECTED",
                    "HUMAN_LABELS_NOT_SUBMITTED",
                    version.bundleDigest(),
                    version.finalSelectedMutationCount());
        }
        List<RankerRecommendation> items = history.forCase(caseId);
        RankerRecommendation latest = items.isEmpty() ? null : items.get(items.size() - 1);
        return new ResultView(
                caseId,
                version.rankerVersion(),
                version.promotionStatus(),
                latest == null ? RankerRecommendationStatus.NEEDS_HUMAN_REVIEW
                        : latest.recommendationStatus(),
                HumanReviewStatus.NOT_STARTED,
                "PROVISIONAL_SELECTED",
                latest == null ? "NO_PRECOMPUTED_RECOMMENDATION" : null,
                version.bundleDigest(),
                version.finalSelectedMutationCount());
    }

    public record ResultView(
            String caseId,
            String rankerVersion,
            RankerPromotionStatus rankerPromotionStatus,
            RankerRecommendationStatus recommendationStatus,
            HumanReviewStatus humanReviewStatus,
            String publishDecision,
            String blockedReason,
            String bundleDigest,
            int finalSelectedMutationCount) {
    }
}
