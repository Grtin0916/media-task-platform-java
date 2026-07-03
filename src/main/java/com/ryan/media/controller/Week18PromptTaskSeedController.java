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
 * Exposes Mainbase W18 prompt task seed as a Java platform handoff API.
 *
 * The endpoint reads a generated artifact from artifacts/manifests. It does not hard-code
 * prompt tasks inside the controller.
 */
@RestController
public class Week18PromptTaskSeedController {

    private static final Path PROMPT_TASK_SEED_REPORT = Path.of(
            "artifacts",
            "manifests",
            "week18_prompt_task_seed",
            "week18_prompt_task_seed_api_report.json"
    );

    private final ObjectMapper objectMapper;

    public Week18PromptTaskSeedController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/week18/prompt-task-seed")
    public ResponseEntity<JsonNode> getPromptTaskSeed() throws IOException {
        if (!Files.isRegularFile(PROMPT_TASK_SEED_REPORT)) {
            JsonNode error = objectMapper.createObjectNode()
                    .put("error", "week18_prompt_task_seed_report_missing")
                    .put("expectedPath", PROMPT_TASK_SEED_REPORT.toString());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        JsonNode body = objectMapper.readTree(PROMPT_TASK_SEED_REPORT.toFile());
        return ResponseEntity.ok(body);
    }
}