package com.ryan.media.week12;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week12AudioTimingRuntimeContractController {

    private final ObjectMapper objectMapper;

    public Week12AudioTimingRuntimeContractController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/week12/audio-timing-runtime")
    public ResponseEntity<JsonNode> runtimeContract() throws IOException {
        return ResponseEntity.ok(loadRuntimeIndex());
    }

    @GetMapping("/api/week12/audio-timing-runtime/event-local-offsets")
    public ResponseEntity<JsonNode> eventLocalOffsets() throws IOException {
        return ResponseEntity.ok(loadRuntimeIndex().path("eventLocalPlacementOffsets"));
    }

    @GetMapping("/api/week12/audio-timing-runtime/placement-required")
    public ResponseEntity<JsonNode> placementRequired() throws IOException {
        return ResponseEntity.ok(loadRuntimeIndex().path("runtimeSemantics").path("eventLocalMode"));
    }

    private JsonNode loadRuntimeIndex() throws IOException {
        ClassPathResource resource = new ClassPathResource("week12/week12_cloud_mainbase_audio_timing_runtime_index.json");
        return objectMapper.readTree(resource.getInputStream());
    }
}
