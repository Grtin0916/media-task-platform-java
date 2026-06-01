package com.ryan.media;

import com.ryan.media.model.MediaTaskAssetBlueprintBinding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTaskAssetBlueprintBindingTest {

    @Test
    void createsWeek12BlueprintV1BindingFromTaskAndInputAsset() {
        MediaTaskAssetBlueprintBinding binding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                "task-week12-001",
                "file://samples/week12/city_walk.mp4",
                "blueprint_v1_66d315251e",
                "pass"
        );

        assertEquals("task-week12-001", binding.taskId());
        assertEquals("file://samples/week12/city_walk.mp4", binding.inputAssetUri());
        assertEquals("blueprint_v1_66d315251e", binding.blueprintId());
        assertEquals("PASS", binding.qualityGateStatus());
        assertTrue(binding.isQualityGatePassed());
        assertTrue(binding.pointsToBlueprintManifest());
        assertTrue(binding.pointsToTimelineArtifact());
        assertEquals(
                "artifacts/manifests/week12_blueprint_v1_manifest.json#blueprint_v1_66d315251e",
                binding.blueprintArtifactUri()
        );
        assertEquals(
                "artifacts/manifests/week12_event_timeline.jsonl",
                binding.timelineArtifactUri()
        );
    }

    @Test
    void rejectsBlankTaskId() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                        " ",
                        "file://samples/week12/city_walk.mp4",
                        "blueprint_v1_66d315251e",
                        "PASS"
                )
        );

        assertEquals("taskId must not be blank", ex.getMessage());
    }

    @Test
    void rejectsBlankInputAssetUri() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                        "task-week12-001",
                        "",
                        "blueprint_v1_66d315251e",
                        "PASS"
                )
        );

        assertEquals("inputAssetUri must not be blank", ex.getMessage());
    }

    @Test
    void rejectsBlankBlueprintId() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                        "task-week12-001",
                        "file://samples/week12/city_walk.mp4",
                        " ",
                        "PASS"
                )
        );

        assertEquals("blueprintId must not be blank", ex.getMessage());
    }

    @Test
    void rejectsUnknownQualityGateStatus() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                        "task-week12-001",
                        "file://samples/week12/city_walk.mp4",
                        "blueprint_v1_66d315251e",
                        "UNKNOWN"
                )
        );

        assertEquals("qualityGateStatus must be one of PASS, WARN, FAIL", ex.getMessage());
    }

    @Test
    void acceptsWarnAndFailAsExplicitNonPassingStates() {
        MediaTaskAssetBlueprintBinding warnBinding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                "task-week12-warn",
                "file://samples/week12/rain_window.mp4",
                "blueprint_v1_276cc4a44c",
                "WARN"
        );

        MediaTaskAssetBlueprintBinding failBinding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                "task-week12-fail",
                "file://samples/week12/door_close.mp4",
                "blueprint_v1_a221f75cba",
                "FAIL"
        );

        assertFalse(warnBinding.isQualityGatePassed());
        assertFalse(failBinding.isQualityGatePassed());
        assertEquals("WARN", warnBinding.qualityGateStatus());
        assertEquals("FAIL", failBinding.qualityGateStatus());
    }

    @Test
    void detectsMalformedManualBlueprintArtifactUri() {
        MediaTaskAssetBlueprintBinding binding = new MediaTaskAssetBlueprintBinding(
                "task-week12-manual",
                "file://samples/week12/keyboard_typing.mp4",
                "artifacts/manifests/other_manifest.json#blueprint_v1_2b95ab765d",
                "blueprint_v1_2b95ab765d",
                "artifacts/manifests/week12_event_timeline.jsonl",
                "PASS"
        );

        assertFalse(binding.pointsToBlueprintManifest());
        assertTrue(binding.pointsToTimelineArtifact());
    }
}