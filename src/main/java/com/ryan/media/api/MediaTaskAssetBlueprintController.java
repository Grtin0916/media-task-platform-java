package com.ryan.media.api;

import com.ryan.media.model.MediaTaskAssetBlueprintBinding;
import com.ryan.media.model.MediaTaskAssetBlueprintView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Week12 minimal HTTP boundary for exposing task -> input asset -> Blueprint V1 links.
 *
 * Boundary:
 * - This endpoint is a deterministic local API adapter.
 * - It does not claim durable persistence, object-store retention, signed URL lifecycle, or queue execution.
 * - It bridges the already-validated domain binding and API view into an HTTP-facing controller shape.
 */
@RestController
public class MediaTaskAssetBlueprintController {

    public static final String DEFAULT_INPUT_ASSET_URI = "file://samples/week12/city_walk.mp4";
    public static final String DEFAULT_BLUEPRINT_ID = "blueprint_v1_66d315251e";
    public static final String DEFAULT_QUALITY_GATE_STATUS = "PASS";

    @GetMapping("/api/media-tasks/{taskId}/asset-blueprint")
    public MediaTaskAssetBlueprintView getTaskAssetBlueprint(
            @PathVariable String taskId,
            @RequestParam(defaultValue = DEFAULT_INPUT_ASSET_URI) String inputAssetUri,
            @RequestParam(defaultValue = DEFAULT_BLUEPRINT_ID) String blueprintId,
            @RequestParam(defaultValue = DEFAULT_QUALITY_GATE_STATUS) String qualityGateStatus
    ) {
        MediaTaskAssetBlueprintBinding binding = MediaTaskAssetBlueprintBinding.week12BlueprintV1(
                taskId,
                inputAssetUri,
                blueprintId,
                qualityGateStatus
        );

        return MediaTaskAssetBlueprintView.fromBinding(binding);
    }
}