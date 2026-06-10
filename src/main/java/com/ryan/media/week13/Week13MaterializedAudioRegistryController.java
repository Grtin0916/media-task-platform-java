package com.ryan.media.week13;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Week13 focused read-only endpoint.
 *
 * Boundary:
 * - consumes the generated materialized readiness report;
 * - does not claim durable registry persistence;
 * - does not claim production object storage;
 * - does not orchestrate workers yet.
 */
@RestController
public class Week13MaterializedAudioRegistryController {

    private static final Path READINESS_REPORT = Path.of(
        "artifacts",
        "manifests",
        "week13_java_materialized_audio_registry_readiness_report.json"
    );

    private final ObjectMapper objectMapper;

    public Week13MaterializedAudioRegistryController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/week13/audio-artifact-registry/materialized-readiness")
    public ResponseEntity<Map<String, Object>> getMaterializedReadiness() throws IOException {
        if (!Files.exists(READINESS_REPORT)) {
            Map<String, Object> missing = new LinkedHashMap<>();
            missing.put("status", "FAIL");
            missing.put("scope", "week13_java_materialized_readiness_api_v1");
            missing.put("reason", "readiness_report_missing");
            missing.put("path", READINESS_REPORT.toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(missing);
        }

        Map<String, Object> report = objectMapper.readValue(
            READINESS_REPORT.toFile(),
            new TypeReference<Map<String, Object>>() {}
        );
        report.put("apiScope", "week13_java_materialized_readiness_api_v1");
        report.put("apiBoundary", "read-only report-backed contract; not durable registry persistence");
        return ResponseEntity.ok(report);
    }
}
