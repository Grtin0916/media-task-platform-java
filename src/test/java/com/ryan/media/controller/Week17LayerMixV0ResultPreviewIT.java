package com.ryan.media.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
class Week17LayerMixV0ResultPreviewIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeLayerMixV0ResultPreview() throws Exception {
        mockMvc.perform(get("/api/week17/layer-mix-v0/result-preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schemaVersion").value("week17.layer_mix_v0.result_preview_api.v1"))
            .andExpect(jsonPath("$.sourceMainbaseHead").value("d9a6df0"))
            .andExpect(jsonPath("$.decision").value("PASS_WEEK17_LAYER_MIX_V0_PLACEHOLDER_CONTROL"))
            .andExpect(jsonPath("$.trackTotal").value(7))
            .andExpect(jsonPath("$.selectedControlIds[0]").value("0001"))
            .andExpect(jsonPath("$.selectedControlIds[6]").value("0009"))
            .andExpect(jsonPath("$.blockedInputIds[0]").value("0004"))
            .andExpect(jsonPath("$.blockedInputIds[2]").value("0010"))
            .andExpect(jsonPath("$.mixArtifactPath").value("artifacts/audio/week17_layer_mix_v0/week17_layer_mix_v0_placeholder_control_mix.wav"))
            .andExpect(jsonPath("$.finalClipRateBeforeClip").value(0.0))
            .andExpect(jsonPath("$.placeholderInputOnly").value(true))
            .andExpect(jsonPath("$.realCandidateAudioClaimed").value(false))
            .andExpect(jsonPath("$.semanticAudioQualityPassClaimed").value(false))
            .andExpect(jsonPath("$.humanReviewPassClaimed").value(false))
            .andExpect(jsonPath("$.finalMixReadinessClaimed").value(false))
            .andExpect(jsonPath("$.productionMixerAvailabilityClaimed").value(false))
            .andExpect(jsonPath("$.platformConsumable").value(true))
            .andExpect(jsonPath("$.readOnlyPreview").value(true))
            .andExpect(jsonPath("$.realWorkerTriggered").value(false))
            .andExpect(jsonPath("$.databasePersistenceClaimed").value(false));
    }
}
