package com.ryan.media.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week17LayerMixV0ResultPreviewController {

    @GetMapping("/api/week17/layer-mix-v0/result-preview")
    public Map<String, Object> getLayerMixV0ResultPreview() {
        return Map.ofEntries(
            Map.entry("schemaVersion", "week17.layer_mix_v0.result_preview_api.v1"),
            Map.entry("sourceMainbaseHead", "d9a6df0"),
            Map.entry("sourceMainbaseManifest", "artifacts/evals/week17_layer_mix_v0_manifest.json"),
            Map.entry("decision", "PASS_WEEK17_LAYER_MIX_V0_PLACEHOLDER_CONTROL"),
            Map.entry("trackTotal", 7),
            Map.entry("selectedControlIds", List.of("0001", "0002", "0003", "0005", "0006", "0008", "0009")),
            Map.entry("blockedInputIds", List.of("0004", "0007", "0010")),
            Map.entry("mixArtifactPath", "artifacts/audio/week17_layer_mix_v0/week17_layer_mix_v0_placeholder_control_mix.wav"),
            Map.entry("finalPeak", 0.8912509083747864),
            Map.entry("finalRms", 0.15652422606945038),
            Map.entry("finalClipRateBeforeClip", 0.0),
            Map.entry("placeholderInputOnly", true),
            Map.entry("realCandidateAudioClaimed", false),
            Map.entry("semanticAudioQualityPassClaimed", false),
            Map.entry("humanReviewPassClaimed", false),
            Map.entry("finalMixReadinessClaimed", false),
            Map.entry("productionMixerAvailabilityClaimed", false),
            Map.entry("blockedClaims", List.of("realCandidateAudioClaimed", "semanticAudioQualityPassClaimed", "humanReviewPassClaimed", "finalMixReadinessClaimed", "productionMixerAvailabilityClaimed")),
            Map.entry("platformConsumable", true),
            Map.entry("readOnlyPreview", true),
            Map.entry("realWorkerTriggered", false),
            Map.entry("databasePersistenceClaimed", false)
        );
    }
}
