package com.ryan.media.week18;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/week18/evaluation")
public class W18EvaluationHandoffController {
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public W18EvaluationHandoffController(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("week", "week18");
        body.put("phase", "dss_vs_naive_evaluation");
        body.put("generatedAt", Instant.now().toString());
        body.put("claimBoundary", "Artifact-backed local evaluation handoff only. No production SLO, k6 threshold pass, or live Grafana import is claimed.");
        body.put("closure", readJson("classpath:week18/eval/w18_eval_closure_20260707.json"));
        body.put("audioMetrics", readJson("classpath:week18/eval/w18_audio_metrics_eval_20260707.json"));
        body.put("pairwise", readJson("classpath:week18/eval/w18_dss_vs_naive_pairwise_report_20260707.json"));
        body.put("selector", readJson("classpath:week18/eval/w18_dss_aware_selector_eval_20260707.json"));
        body.put("repairSeed", readJson("classpath:week18/eval/w18_repair_aware_selector_seed_20260707.json"));
        return body;
    }

    private Object readJson(String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            try (InputStream in = resource.getInputStream()) {
                return objectMapper.readValue(in, Object.class);
            }
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("resource", location);
            error.put("error", e.getClass().getSimpleName());
            error.put("message", e.getMessage());
            return error;
        }
    }
}
