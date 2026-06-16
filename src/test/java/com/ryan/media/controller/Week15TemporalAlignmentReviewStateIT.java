package com.ryan.media.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week15TemporalAlignmentReviewStateIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void shouldExposeTemporalAlignmentReviewStateWithoutOverclaiming() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/week15/temporal-alignment-review-state",
                Map.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        Map body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("schemaVersion")).isEqualTo("week15.temporal-alignment-review-state.v0");
        assertThat(body.get("platformDecision")).isEqualTo("SEMANTIC_REVIEW_READY_FOR_PLATFORM");
        assertThat(body.get("claimBoundaryOk")).isEqualTo(Boolean.TRUE);
        assertThat(body.get("qualityGateLiteStatus")).isEqualTo("SEMANTIC_REVIEW_READY");

        Map reviewBoundary = (Map) body.get("reviewBoundary");
        assertThat(reviewBoundary.get("humanReviewStatus")).isEqualTo("HUMAN_REVIEW_PARTIAL");
        assertThat(reviewBoundary.get("auditionStatus")).isEqualTo("NOT_PERFORMED");
        assertThat(reviewBoundary.get("semanticQualityReviewStatus")).isEqualTo("NOT_PERFORMED");
        assertThat(reviewBoundary.get("finalMixReadiness")).isEqualTo("NOT_CLAIMED");

        Map riskSummary = (Map) body.get("riskSummary");
        assertThat(riskSummary.get("candidateCount")).isEqualTo(10);
        assertThat(riskSummary.get("candidatesWithOriginalAudio")).isEqualTo(10);
        assertThat(riskSummary.get("candidatesWithRemediatedAudio")).isEqualTo(2);
        assertThat(riskSummary.get("riskyCandidateCount")).isEqualTo(4);

        List riskCandidateIds = (List) riskSummary.get("riskCandidateIds");
        assertThat(riskCandidateIds).contains("procedural_v0_0004");

        List riskDrilldown = (List) body.get("riskDrilldown");
        assertThat(riskDrilldown).isNotEmpty();

        Map candidate0004 = null;
        for (Object item : riskDrilldown) {
            Map candidate = (Map) item;
            if ("procedural_v0_0004".equals(candidate.get("candidateId"))) {
                candidate0004 = candidate;
                break;
            }
        }

        assertThat(candidate0004).isNotNull();
        assertThat((List) candidate0004.get("riskFlags"))
                .contains("HIGH_LOW_ENERGY_RATIO_REVIEW_EVENT_PRESENCE");
        assertThat((List) candidate0004.get("artifactPaths"))
                .contains(
                        "artifacts/audio_candidates/week12_procedural_baseline_v0/0004_slot_v0_0004_generation_fallback.wav",
                        "artifacts/audio_candidates/week15_temporal_alignment_remediated/procedural_v0_0004_trimmed_preroll_20ms.wav"
                );

        Map artifactLinks = (Map) body.get("artifactLinks");
        assertThat(artifactLinks.get("mainbaseSemanticQualityReviewJson").toString())
                .contains("week15_temporal_alignment_semantic_quality_review_v0.json");
        assertThat(artifactLinks.get("cloudSemanticQualityPlatformIndex").toString())
                .contains("week15_temporal_alignment_semantic_quality_platform_index.json");
        assertThat(artifactLinks.get("cloudSemanticQualityPrometheusMetrics").toString())
                .contains("week15_temporal_alignment_semantic_quality_metrics.prom");

        List blockedClaims = (List) body.get("blockedClaims");
        assertThat(blockedClaims).contains(
                "Do not claim human audition PASS.",
                "Do not claim semantic audio quality PASS.",
                "Do not claim final mix readiness."
        );
    }
}
