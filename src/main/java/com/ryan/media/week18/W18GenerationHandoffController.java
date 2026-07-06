package com.ryan.media.week18;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/week18/generation", produces = MediaType.APPLICATION_JSON_VALUE)
public class W18GenerationHandoffController {

    private static final String MANIFEST_PATH =
            "week18/w18_java_cloud_handoff_manifest_20260706.json";

    private final ObjectMapper objectMapper;
    private JsonNode cachedManifest;

    public W18GenerationHandoffController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private synchronized JsonNode manifest() throws IOException {
        if (cachedManifest == null) {
            ClassPathResource resource = new ClassPathResource(MANIFEST_PATH);
            String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            cachedManifest = objectMapper.readTree(json);
        }
        return cachedManifest;
    }

    @GetMapping("/health")
    public ResponseEntity<JsonNode> health() throws IOException {
        JsonNode manifest = manifest();
        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", "UP");
        node.put("contractVersion", manifest.path("contract_version").asText());
        node.put("handoffStatus", manifest.path("status").asText());
        node.put("caseCount", manifest.path("summary").path("case_count").asInt());
        node.put("generatedCount", manifest.path("summary").path("generated_count").asInt());
        node.put("repairAppliedCount", manifest.path("summary").path("repair_applied_count").asInt());
        return ResponseEntity.ok(node);
    }

    @GetMapping("/handoff")
    public ResponseEntity<JsonNode> handoff() throws IOException {
        return ResponseEntity.ok(manifest());
    }

    @GetMapping("/summary")
    public ResponseEntity<JsonNode> summary() throws IOException {
        JsonNode manifest = manifest();
        ObjectNode node = objectMapper.createObjectNode();
        node.set("summary", manifest.path("summary"));
        node.put("status", manifest.path("status").asText());
        node.put("contractVersion", manifest.path("contract_version").asText());
        node.set("claimBoundary", manifest.path("claim_boundary"));
        return ResponseEntity.ok(node);
    }

    @GetMapping("/cases")
    public ResponseEntity<JsonNode> cases() throws IOException {
        return ResponseEntity.ok(manifest().path("cases"));
    }

    @GetMapping("/cases/{caseId}")
    public ResponseEntity<JsonNode> caseById(@PathVariable String caseId) throws IOException {
        for (JsonNode item : manifest().path("cases")) {
            if (caseId.equals(item.path("case_id").asText())) {
                return ResponseEntity.ok(item);
            }
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", "case_not_found");
        error.put("caseId", caseId);
        return ResponseEntity.status(404).body(error);
    }

    @GetMapping("/default-candidates")
    public ResponseEntity<JsonNode> defaultCandidates() throws IOException {
        ArrayNode array = objectMapper.createArrayNode();
        for (JsonNode item : manifest().path("cases")) {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("caseId", item.path("case_id").asText());
            row.put("defaultCandidateVariant", item.path("default_candidate_variant").asText());
            row.put("defaultCandidateAudioPath", item.path("default_candidate_audio_path").asText());
            row.set("reviewVariants", item.path("review_variants"));
            row.put("variantCount", item.path("variant_count").asInt());
            row.put("generatedCount", item.path("generated_count").asInt());
            array.add(row);
        }
        return ResponseEntity.ok(array);
    }
}
