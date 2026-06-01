package com.ryan.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryan.media.api.MediaTaskAssetBlueprintController;
import com.ryan.media.model.MediaTaskAssetBlueprintView;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MediaTaskAssetBlueprintControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsDefaultWeek12BlueprintViewForTask() {
        MediaTaskAssetBlueprintController controller = new MediaTaskAssetBlueprintController();

        MediaTaskAssetBlueprintView view = controller.getTaskAssetBlueprint(
                "task-week12-001",
                MediaTaskAssetBlueprintController.DEFAULT_INPUT_ASSET_URI,
                MediaTaskAssetBlueprintController.DEFAULT_BLUEPRINT_ID,
                MediaTaskAssetBlueprintController.DEFAULT_QUALITY_GATE_STATUS
        );

        assertEquals("task-week12-001", view.taskId());
        assertEquals("file://samples/week12/city_walk.mp4", view.inputAssetUri());
        assertEquals("blueprint_v1_66d315251e", view.blueprintId());
        assertEquals(
                "artifacts/manifests/week12_blueprint_v1_manifest.json#blueprint_v1_66d315251e",
                view.blueprintArtifactUri()
        );
        assertEquals("artifacts/manifests/week12_event_timeline.jsonl", view.timelineArtifactUri());
        assertEquals("PASS", view.qualityGateStatus());
        assertTrue(view.qualityGatePassed());
        assertTrue(view.blueprintManifestLinked());
        assertTrue(view.timelineArtifactLinked());
        assertTrue(view.readyForApiExposure());
        assertTrue(view.readyForSucceededTaskExposure());
    }

    @Test
    void returnsCustomBlueprintViewForTask() {
        MediaTaskAssetBlueprintController controller = new MediaTaskAssetBlueprintController();

        MediaTaskAssetBlueprintView view = controller.getTaskAssetBlueprint(
                "task-week12-rain",
                "file://samples/week12/rain_window.mp4",
                "blueprint_v1_276cc4a44c",
                "WARN"
        );

        assertEquals("task-week12-rain", view.taskId());
        assertEquals("file://samples/week12/rain_window.mp4", view.inputAssetUri());
        assertEquals("blueprint_v1_276cc4a44c", view.blueprintId());
        assertEquals("WARN", view.qualityGateStatus());
        assertFalse(view.qualityGatePassed());
        assertTrue(view.readyForApiExposure());
        assertFalse(view.readyForSucceededTaskExposure());
    }

    @Test
    void serializesControllerResponseShapeAsStableJson() throws Exception {
        MediaTaskAssetBlueprintController controller = new MediaTaskAssetBlueprintController();

        MediaTaskAssetBlueprintView view = controller.getTaskAssetBlueprint(
                "task-week12-001",
                "file://samples/week12/city_walk.mp4",
                "blueprint_v1_66d315251e",
                "PASS"
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(view));

        assertEquals("task-week12-001", json.get("taskId").asText());
        assertEquals("file://samples/week12/city_walk.mp4", json.get("inputAssetUri").asText());
        assertEquals(
                "artifacts/manifests/week12_blueprint_v1_manifest.json#blueprint_v1_66d315251e",
                json.get("blueprintArtifactUri").asText()
        );
        assertEquals("blueprint_v1_66d315251e", json.get("blueprintId").asText());
        assertEquals("artifacts/manifests/week12_event_timeline.jsonl", json.get("timelineArtifactUri").asText());
        assertEquals("PASS", json.get("qualityGateStatus").asText());
        assertTrue(json.get("qualityGatePassed").asBoolean());
        assertTrue(json.get("blueprintManifestLinked").asBoolean());
        assertTrue(json.get("timelineArtifactLinked").asBoolean());
    }

    @Test
    void mockMvcDispatchesDefaultGetEndpointAsJson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MediaTaskAssetBlueprintController())
                .build();

        mockMvc.perform(get("/api/media-tasks/{taskId}/asset-blueprint", "task-week12-001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taskId").value("task-week12-001"))
                .andExpect(jsonPath("$.inputAssetUri").value("file://samples/week12/city_walk.mp4"))
                .andExpect(jsonPath("$.blueprintId").value("blueprint_v1_66d315251e"))
                .andExpect(jsonPath("$.blueprintArtifactUri").value(
                        "artifacts/manifests/week12_blueprint_v1_manifest.json#blueprint_v1_66d315251e"))
                .andExpect(jsonPath("$.timelineArtifactUri").value("artifacts/manifests/week12_event_timeline.jsonl"))
                .andExpect(jsonPath("$.qualityGateStatus").value("PASS"))
                .andExpect(jsonPath("$.qualityGatePassed").value(true))
                .andExpect(jsonPath("$.blueprintManifestLinked").value(true))
                .andExpect(jsonPath("$.timelineArtifactLinked").value(true));
    }

    @Test
    void mockMvcDispatchesCustomQueryParamsAsJson() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MediaTaskAssetBlueprintController())
                .build();

        mockMvc.perform(get("/api/media-tasks/{taskId}/asset-blueprint", "task-week12-rain")
                        .param("inputAssetUri", "file://samples/week12/rain_window.mp4")
                        .param("blueprintId", "blueprint_v1_276cc4a44c")
                        .param("qualityGateStatus", "WARN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taskId").value("task-week12-rain"))
                .andExpect(jsonPath("$.inputAssetUri").value("file://samples/week12/rain_window.mp4"))
                .andExpect(jsonPath("$.blueprintId").value("blueprint_v1_276cc4a44c"))
                .andExpect(jsonPath("$.blueprintArtifactUri").value(
                        "artifacts/manifests/week12_blueprint_v1_manifest.json#blueprint_v1_276cc4a44c"))
                .andExpect(jsonPath("$.timelineArtifactUri").value("artifacts/manifests/week12_event_timeline.jsonl"))
                .andExpect(jsonPath("$.qualityGateStatus").value("WARN"))
                .andExpect(jsonPath("$.qualityGatePassed").value(false))
                .andExpect(jsonPath("$.blueprintManifestLinked").value(true))
                .andExpect(jsonPath("$.timelineArtifactLinked").value(true));
    }

    @Test
    void rejectsBlankTaskIdThroughDomainBinding() {
        MediaTaskAssetBlueprintController controller = new MediaTaskAssetBlueprintController();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getTaskAssetBlueprint(
                        " ",
                        "file://samples/week12/city_walk.mp4",
                        "blueprint_v1_66d315251e",
                        "PASS"
                )
        );

        assertEquals("taskId must not be blank", ex.getMessage());
    }

    @Test
    void rejectsInvalidQualityGateStatusThroughDomainBinding() {
        MediaTaskAssetBlueprintController controller = new MediaTaskAssetBlueprintController();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getTaskAssetBlueprint(
                        "task-week12-001",
                        "file://samples/week12/city_walk.mp4",
                        "blueprint_v1_66d315251e",
                        "UNKNOWN"
                )
        );

        assertEquals("qualityGateStatus must be one of PASS, WARN, FAIL", ex.getMessage());
    }
}