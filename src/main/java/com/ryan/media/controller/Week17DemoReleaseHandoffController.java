package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Mainbase Week17 true-aware demo release candidate as a Java handoff API.
 *
 * This controller reads a generated artifact from artifacts/manifests instead of returning a
 * hard-coded static payload. It is intentionally a demo handoff endpoint, not a production
 * availability claim.
 */
@RestController
public class Week17DemoReleaseHandoffController {

    private static final Path HANDOFF_REPORT = Path.of(
            "artifacts",
            "manifests",
            "week17_demo_release_handoff",
            "week17_demo_release_handoff_report.json"
    );

    private final ObjectMapper objectMapper;

    public Week17DemoReleaseHandoffController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/week17/demo-release-handoff")
    public ResponseEntity<JsonNode> getDemoReleaseHandoff() throws IOException {
        if (!Files.isRegularFile(HANDOFF_REPORT)) {
            JsonNode error = objectMapper.createObjectNode()
                    .put("error", "week17_demo_release_handoff_report_missing")
                    .put("expectedPath", HANDOFF_REPORT.toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        JsonNode body = objectMapper.readTree(HANDOFF_REPORT.toFile());
        return ResponseEntity.ok(body);
    }
}