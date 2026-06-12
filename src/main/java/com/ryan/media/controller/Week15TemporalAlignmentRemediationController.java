package com.ryan.media.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week15TemporalAlignmentRemediationController {

    @GetMapping("/api/week15/temporal-alignment-remediation")
    public Map<String, Object> remediationStatus() {
        return Map.of(
            "schemaVersion", "week15.temporal_alignment_remediation_api.v1",
            "status", "PASS",
            "gateDecision", "TEMPORAL_ALIGNMENT_REMEDIATION_REGRESSION_GUARDED",
            "source", Map.of(
                "mainbaseHead", "c0b4870",
                "regressionGate", "artifacts/evals/week15_temporal_alignment_regression_gate.json",
                "originalSummary", "artifacts/evals/week15_temporal_alignment_summary.json",
                "remediationPlan", "artifacts/evals/week15_temporal_alignment_remediation_plan.json",
                "remediatedSummary", "artifacts/evals/week15_temporal_alignment_remediated_summary.json"
            ),
            "original", Map.of(
                "candidateCount", 10,
                "passCount", 7,
                "warnNearMissCount", 1,
                "failCount", 2,
                "eventLocalCount", 5,
                "eventLocalPassCount", 3
            ),
            "remediated", Map.of(
                "candidateCount", 10,
                "passCount", 9,
                "warnNearMissCount", 1,
                "failCount", 0,
                "eventLocalCount", 5,
                "eventLocalPassCount", 5
            ),
            "improvement", Map.of(
                "eventLocalPassDelta", 2,
                "failCountDelta", -2,
                "remediatedCandidateIds", List.of("procedural_v0_0004", "procedural_v0_0010")
            ),
            "artifactLinks", Map.of(
                "remediatedAudio0004", "artifacts/audio_candidates/week15_temporal_alignment_remediated/procedural_v0_0004_trimmed_preroll_20ms.wav",
                "remediatedAudio0010", "artifacts/audio_candidates/week15_temporal_alignment_remediated/procedural_v0_0010_trimmed_preroll_20ms.wav"
            ),
            "boundary", List.of(
                "api_contract_only",
                "consumes_mainbase_artifact_evidence",
                "does_not_score_semantic_audio_quality",
                "does_not_claim_human_audition_passed",
                "does_not_claim_final_mix_readiness",
                "does_not_claim_production_slo"
            )
        );
    }
}