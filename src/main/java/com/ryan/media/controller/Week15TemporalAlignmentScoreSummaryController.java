package com.ryan.media.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week15TemporalAlignmentScoreSummaryController {

    @GetMapping("/api/week15/temporal-alignment-score-summary")
    public Map<String, Object> scoreSummary() {
        return linkedMap(
            "schemaVersion", "week15.temporal_alignment_score_summary_api.v1",
            "status", "PASS",
            "gateDecision", "TEMPORAL_ALIGNMENT_DASHBOARD_READY_AND_REGRESSION_GUARDED",
            "source", linkedMap(
                "mainbaseHead", "c0b4870",
                "javaInputHead", "d09b4f5",
                "cloudHead", "94341d9",
                "diagnosticsJson", "artifacts/evals/week15_temporal_alignment_diagnostics.json",
                "diagnosticsCsv", "artifacts/evals/week15_temporal_alignment_diagnostics.csv",
                "waveformIndex", "artifacts/evals/week15_temporal_alignment_waveform_index.json",
                "regressionGate", "artifacts/evals/week15_temporal_alignment_regression_gate.json",
                "cloudDashboardReady", "loadtest/reports/week15_temporal_alignment_dashboard_ready.json"
            ),
            "scoreSummary", linkedMap(
                "candidateCount", 10,
                "originalPassCount", 7,
                "originalWarnNearMissCount", 1,
                "originalFailCount", 2,
                "remediatedPassCount", 9,
                "remediatedWarnNearMissCount", 1,
                "remediatedFailCount", 0,
                "eventLocalCount", 5,
                "originalEventLocalPassCount", 3,
                "remediatedEventLocalPassCount", 5
            ),
            "remediationDelta", linkedMap(
                "eventLocalPassDelta", 2,
                "failCountDelta", -2,
                "remediatedCandidateIds", List.of("procedural_v0_0004", "procedural_v0_0010"),
                "remediationAction", "trim_leading_low_energy_with_20ms_preroll"
            ),
            "candidateDrifts", List.of(
                linkedMap(
                    "candidateId", "procedural_v0_0004",
                    "originalStatus", "FAIL_DRIFT",
                    "remediatedStatus", "PASS",
                    "failureMode", "leading_low_energy_or_silence_before_local_onset",
                    "remediationAction", "trim_leading_low_energy_with_20ms_preroll",
                    "requiresHumanAudition", true
                ),
                linkedMap(
                    "candidateId", "procedural_v0_0010",
                    "originalStatus", "FAIL_DRIFT",
                    "remediatedStatus", "PASS",
                    "failureMode", "leading_low_energy_or_silence_before_local_onset",
                    "remediationAction", "trim_leading_low_energy_with_20ms_preroll",
                    "requiresHumanAudition", true
                )
            ),
            "artifactLinks", linkedMap(
                "remediatedAudio0004", "artifacts/audio_candidates/week15_temporal_alignment_remediated/procedural_v0_0004_trimmed_preroll_20ms.wav",
                "remediatedAudio0010", "artifacts/audio_candidates/week15_temporal_alignment_remediated/procedural_v0_0010_trimmed_preroll_20ms.wav",
                "mainbaseDiagnostics", "artifacts/evals/week15_temporal_alignment_diagnostics.json",
                "cloudDashboard", "observability/grafana/dashboards/week15_temporal_alignment_remediation_dashboard.json",
                "cloudReadyReport", "loadtest/reports/week15_temporal_alignment_dashboard_ready.json"
            ),
            "boundary", List.of(
                "api_contract_only",
                "consumes_mainbase_and_cloud_artifact_evidence",
                "does_not_score_semantic_audio_quality",
                "does_not_claim_human_audition_passed",
                "does_not_claim_final_mix_readiness",
                "does_not_claim_live_grafana_import",
                "does_not_claim_production_slo"
            )
        );
    }

    private static Map<String, Object> linkedMap(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("linkedMap requires an even number of arguments");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
