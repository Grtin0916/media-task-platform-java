package com.ryan.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryan.media.model.MediaTaskAssetBlueprintBinding;
import com.ryan.media.model.MediaTaskAssetBlueprintView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTaskAssetBlueprintViewTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesBindingAsApiReadyView() {
        MediaTaskAssetBlueprintBinding binding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                "task-week12-001",
                "file://samples/week12/city_walk.mp4",
                "blueprint_v1_66d315251e",
                "PASS"
        );

        MediaTaskAssetBlueprintView view = MediaTaskAssetBlueprintView.fromBinding(binding);

        assertEquals("task-week12-001", view.taskId());
        assertEquals("file://samples/week12/city_walk.mp4", view.inputAssetUri());
        assertEquals(
                "artifacts/manifests/week12_blueprint_v1_manifest.json#blueprint_v1_66d315251e",
                view.blueprintArtifactUri()
        );
        assertEquals("blueprint_v1_66d315251e", view.blueprintId());
        assertEquals("artifacts/manifests/week12_event_timeline.jsonl", view.timelineArtifactUri());
        assertEquals("PASS", view.qualityGateStatus());
        assertTrue(view.qualityGatePassed());
        assertTrue(view.blueprintManifestLinked());
        assertTrue(view.timelineArtifactLinked());
        assertTrue(view.readyForApiExposure());
        assertTrue(view.readyForSucceededTaskExposure());
    }

    @Test
    void serializesStableJsonFieldsForApiContract() throws Exception {
        MediaTaskAssetBlueprintBinding binding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                "task-week12-001",
                "file://samples/week12/city_walk.mp4",
                "blueprint_v1_66d315251e",
                "PASS"
        );

        MediaTaskAssetBlueprintView view = MediaTaskAssetBlueprintView.fromBinding(binding);
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
    void warnQualityGateIsApiVisibleButNotSucceededReady() {
        MediaTaskAssetBlueprintBinding binding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                "task-week12-warn",
                "file://samples/week12/rain_window.mp4",
                "blueprint_v1_276cc4a44c",
                "WARN"
        );

        MediaTaskAssetBlueprintView view = MediaTaskAssetBlueprintView.fromBinding(binding);

        assertEquals("WARN", view.qualityGateStatus());
        assertFalse(view.qualityGatePassed());
        assertTrue(view.readyForApiExposure());
        assertFalse(view.readyForSucceededTaskExposure());
    }

    @Test
    void malformedManifestLinkIsVisibleButNotApiReady() {
        MediaTaskAssetBlueprintBinding binding = new MediaTaskAssetBlueprintBinding(
                "task-week12-malformed",
                "file://samples/week12/keyboard_typing.mp4",
                "artifacts/manifests/other_manifest.json#blueprint_v1_2b95ab765d",
                "blueprint_v1_2b95ab765d",
                "artifacts/manifests/week12_event_timeline.jsonl",
                "PASS"
        );

        MediaTaskAssetBlueprintView view = MediaTaskAssetBlueprintView.fromBinding(binding);

        assertFalse(view.blueprintManifestLinked());
        assertTrue(view.timelineArtifactLinked());
        assertFalse(view.readyForApiExposure());
        assertFalse(view.readyForSucceededTaskExposure());
    }

    @Test
    void rejectsNullBinding() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> MediaTaskAssetBlueprintView.fromBinding(null)
        );

        assertEquals("binding must not be null", ex.getMessage());
    }
}