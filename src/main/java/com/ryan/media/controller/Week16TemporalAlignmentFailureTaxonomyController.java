package com.ryan.media.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week16TemporalAlignmentFailureTaxonomyController {

    static final String ENDPOINT = "/api/week16/temporal-alignment/failure-taxonomy";
    private static final Path PAYLOAD_PATH =
            Path.of("artifacts/manifests/week16_java_temporal_alignment_failure_taxonomy_payload.json");

    private final ObjectMapper objectMapper;

    public Week16TemporalAlignmentFailureTaxonomyController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping(ENDPOINT)
    public ResponseEntity<JsonNode> getFailureTaxonomy() throws IOException {
        if (!Files.exists(PAYLOAD_PATH)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(objectMapper.readTree(PAYLOAD_PATH.toFile()));
    }
}
