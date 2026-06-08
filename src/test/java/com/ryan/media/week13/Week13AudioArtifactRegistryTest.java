package com.ryan.media.week13;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class Week13AudioArtifactRegistryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "week13", roles = {"USER"})
    void shouldExposeMainbaseWeek13PlacementRegistryContract() throws Exception {
        String body = mockMvc.perform(get("/api/week13/audio-artifacts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);

        assertEquals("GENERATED", root.get("status").asText());
        assertEquals("week13_audio_artifact_registry_contract_v0", root.get("scope").asText());
        assertEquals(10, root.get("candidateCount").asInt());
        assertEquals(5, root.get("assetTimeModeCounts").get("full_clip").asInt());
        assertEquals(5, root.get("assetTimeModeCounts").get("event_local").asInt());
        assertEquals(5, root.get("placementRequiredCount").asInt());
        assertEquals(10, root.get("readyCount").asInt());

        int eventLocalCount = 0;
        int fixedPlacementMismatchCount = 0;
        for (JsonNode item : root.get("items")) {
            assertTrue(item.hasNonNull("candidateId"));
            assertTrue(item.hasNonNull("audioUri"));
            assertTrue(item.hasNonNull("assetTimeMode"));
            assertTrue(item.hasNonNull("expectedStartSec"));
            assertTrue(item.hasNonNull("globalStartSec"));
            assertEquals("READY_FOR_RUNTIME_PLACEMENT", item.get("status").asText());

            if ("event_local".equals(item.get("assetTimeMode").asText())) {
                eventLocalCount++;
                assertTrue(item.get("placementRequired").asBoolean());
                BigDecimal expectedStart = new BigDecimal(item.get("expectedStartSec").asText());
                BigDecimal globalStart = new BigDecimal(item.get("globalStartSec").asText());
                BigDecimal offset = new BigDecimal(item.get("placementOffsetSec").asText());
                if (expectedStart.compareTo(globalStart) != 0 || expectedStart.compareTo(offset) != 0) {
                    fixedPlacementMismatchCount++;
                }
            }

            if ("full_clip".equals(item.get("assetTimeMode").asText())) {
                assertFalse(item.get("placementRequired").asBoolean());
                assertEquals(0, new BigDecimal(item.get("globalStartSec").asText()).compareTo(BigDecimal.ZERO));
            }
        }

        assertEquals(5, eventLocalCount);
        assertEquals(0, fixedPlacementMismatchCount);
    }

    @Test
    @WithMockUser(username = "week13", roles = {"USER"})
    void shouldReturnSingleCandidateById() throws Exception {
        String body = mockMvc.perform(get("/api/week13/audio-artifacts/procedural_v0_0002"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode item = objectMapper.readTree(body);
        assertEquals("procedural_v0_0002", item.get("candidateId").asText());
        assertEquals("event_local", item.get("assetTimeMode").asText());
        assertTrue(item.get("placementRequired").asBoolean());
        assertEquals(0, new BigDecimal(item.get("expectedStartSec").asText())
                .compareTo(new BigDecimal(item.get("globalStartSec").asText())));
    }

    @Test
    @WithMockUser(username = "week13", roles = {"USER"})
    void shouldReturnNotFoundForUnknownCandidate() throws Exception {
        mockMvc.perform(get("/api/week13/audio-artifacts/not-a-real-candidate"))
                .andExpect(status().isNotFound());
    }
}
