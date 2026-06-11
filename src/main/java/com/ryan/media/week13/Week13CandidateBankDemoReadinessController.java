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
 * Week13 fixture-backed candidate bank demo readiness endpoint.
 *
 * Boundary:
 * - This endpoint exposes a local demo-readiness contract only.
 * - It does not claim durable registry persistence.
 * - It does not claim semantic audio quality, human audition, final mix readiness,
 *   production Kubernetes Job, or cloud object storage.
 */
@RestController
public class Week13CandidateBankDemoReadinessController {

    private static final Path REPORT_PATH = Path.of(
            "artifacts",
            "manifests",
            "week13_java_candidate_bank_demo_consumption_report.json"
    );

    private final ObjectMapper objectMapper;

    public Week13CandidateBankDemoReadinessController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/week13/candidate-bank-demo-readiness")
    public ResponseEntity<Map<String, Object>> getReadiness() throws IOException {
        if (!Files.exists(REPORT_PATH)) {
            Map<String, Object> missing = new LinkedHashMap<>();
            missing.put("status", "FAIL");
            missing.put("endpoint", "/api/week13/candidate-bank-demo-readiness");
            missing.put("scope", "java-platform-http-readiness-api");
            missing.put("sourceReport", REPORT_PATH.toString());
            missing.put("blockers", java.util.List.of("missing consumption report: " + REPORT_PATH));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(missing);
        }

        Map<String, Object> report = objectMapper.readValue(
                REPORT_PATH.toFile(),
                new TypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("endpoint", "/api/week13/candidate-bank-demo-readiness");
        body.put("status", report.get("status"));
        body.put("scope", "java-platform-http-readiness-api");
        body.put("sourceReport", REPORT_PATH.toString());
        body.put("sourceScope", report.get("scope"));
        body.put("consumedCounts", report.get("consumedCounts"));
        body.put("javaStatuses", report.get("javaStatuses"));
        body.put("hardChecks", report.get("hardChecks"));
        body.put("blockers", report.get("blockers"));
        body.put("boundary", report.get("boundary"));
        body.put("platformDecision", report.get("platformDecision"));
        body.put("nextRecommendedStep", report.get("nextRecommendedStep"));

        boolean pass = "PASS".equals(report.get("status"));
        return ResponseEntity.status(pass ? HttpStatus.OK : HttpStatus.CONFLICT).body(body);
    }
}