package com.ryan.media.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class Week15TemporalAlignmentReviewStateController {

    @GetMapping("/api/week15/temporal-alignment-review-state")
    public ResponseEntity<ReviewStateResponse> getReviewState() {
        ReviewBoundary reviewBoundary = new ReviewBoundary(
                "HUMAN_REVIEW_PARTIAL",
                "NOT_PERFORMED",
                "NOT_PERFORMED",
                "NOT_CLAIMED"
        );

        RiskSummary riskSummary = new RiskSummary(
                10,
                10,
                10,
                2,
                4,
                List.of(
                        "procedural_v0_0002",
                        "procedural_v0_0003",
                        "procedural_v0_0004",
                        "procedural_v0_0007"
                )
        );

        List<RiskCandidate> riskDrilldown = List.of(
                new RiskCandidate(
                        "procedural_v0_0002",
                        List.of("HIGH_LOW_ENERGY_RATIO_REVIEW_EVENT_PRESENCE"),
                        List.of("artifacts/audio_candidates/week12_procedural_baseline_v0/0002_slot_v0_0002_generation_fallback.wav")
                ),
                new RiskCandidate(
                        "procedural_v0_0003",
                        List.of("LOW_PEAK_AMPLITUDE"),
                        List.of("artifacts/audio_candidates/week12_procedural_baseline_v0/0003_slot_v0_0003_generation_fallback.wav")
                ),
                new RiskCandidate(
                        "procedural_v0_0004",
                        List.of("HIGH_LOW_ENERGY_RATIO_REVIEW_EVENT_PRESENCE"),
                        List.of(
                                "artifacts/audio_candidates/week12_procedural_baseline_v0/0004_slot_v0_0004_generation_fallback.wav",
                                "artifacts/audio_candidates/week15_temporal_alignment_remediated/procedural_v0_0004_trimmed_preroll_20ms.wav"
                        )
                ),
                new RiskCandidate(
                        "procedural_v0_0007",
                        List.of("LOW_PEAK_AMPLITUDE"),
                        List.of("artifacts/audio_candidates/week12_procedural_baseline_v0/0007_slot_v0_0007_generation_fallback.wav")
                )
        );

        ArtifactLinks artifactLinks = new ArtifactLinks(
                "mainbase:artifacts/reviews/week15_temporal_alignment_semantic_quality_review_v0.json",
                "cloud:loadtest/reports/week15_temporal_alignment_semantic_quality_platform_index.json",
                "cloud:observability/prometheus/week15_temporal_alignment_semantic_quality_metrics.prom"
        );

        ReviewStateResponse response = new ReviewStateResponse(
                "week15.temporal-alignment-review-state.v0",
                "SEMANTIC_REVIEW_READY_FOR_PLATFORM",
                true,
                "SEMANTIC_REVIEW_READY",
                reviewBoundary,
                riskSummary,
                riskDrilldown,
                artifactLinks,
                List.of(
                        "Java API exposes platform-consumed semantic review state.",
                        "Client can query review boundary, risk summary, and artifact links."
                ),
                List.of(
                        "Do not claim human audition PASS.",
                        "Do not claim semantic audio quality PASS.",
                        "Do not claim final mix readiness.",
                        "Do not claim live Grafana import or production SLO."
                )
        );

        return ResponseEntity.ok(response);
    }

    public record ReviewStateResponse(
            String schemaVersion,
            String platformDecision,
            boolean claimBoundaryOk,
            String qualityGateLiteStatus,
            ReviewBoundary reviewBoundary,
            RiskSummary riskSummary,
            List<RiskCandidate> riskDrilldown,
            ArtifactLinks artifactLinks,
            List<String> allowedClaims,
            List<String> blockedClaims
    ) {
    }

    public record ReviewBoundary(
            String humanReviewStatus,
            String auditionStatus,
            String semanticQualityReviewStatus,
            String finalMixReadiness
    ) {
    }

    public record RiskSummary(
            int candidateCount,
            int candidatesWithAnyAudio,
            int candidatesWithOriginalAudio,
            int candidatesWithRemediatedAudio,
            int riskyCandidateCount,
            List<String> riskCandidateIds
    ) {
    }

    public record RiskCandidate(
            String candidateId,
            List<String> riskFlags,
            List<String> artifactPaths
    ) {
    }

    public record ArtifactLinks(
            String mainbaseSemanticQualityReviewJson,
            String cloudSemanticQualityPlatformIndex,
            String cloudSemanticQualityPrometheusMetrics
    ) {
    }
}
