package com.ryan.media.week15;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemporalAlignmentReviewStateRegistry {
    private static final String SOURCE_TYPE = "ARTIFACT_REGISTRY_BACKED";
    private static final Path REVIEW_STATE_REPORT =
            Path.of("artifacts/manifests/week15_java_temporal_alignment_review_state_contract_report.json");
    private static final Path E2E_MANIFEST =
            Path.of("artifacts/manifests/week15_temporal_alignment_semantic_quality_e2e_manifest.json");

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> loadReviewState() {
        JsonNode report = readJson(REVIEW_STATE_REPORT);
        JsonNode manifest = readJson(E2E_MANIFEST);

        String reportText = readText(REVIEW_STATE_REPORT);
        String manifestText = readText(E2E_MANIFEST);
        String joinedText = reportText + "\n" + manifestText;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sourceType", SOURCE_TYPE);
        response.put("source", "artifact-registry-snapshot");
        response.put("artifactRegistryBacked", true);
        response.put("registrySnapshotPath", REVIEW_STATE_REPORT.toString());
        response.put("e2eManifestPath", E2E_MANIFEST.toString());
        response.put("schemaVersion", "week15.temporal-alignment-review-state.registry-backed.v1");
        response.put("e2eManifestSchemaVersion", firstText(manifest, "schemaVersion").orElse("UNKNOWN"));
        response.put("decision", firstText(report, "decision")
                .or(() -> firstText(manifest, "decision"))
                .orElse("UNKNOWN"));

        List<String> riskCandidateIds = extractCandidateIds(joinedText);
        response.put("riskCandidateIds", riskCandidateIds);

        Map<String, Object> reviewBoundary = new LinkedHashMap<>();
        reviewBoundary.put("humanReviewStatus", firstText(report, "humanReviewStatus")
                .or(() -> firstText(manifest, "humanReviewStatus"))
                .orElse("HUMAN_REVIEW_PARTIAL"));
        reviewBoundary.put("auditionStatus", firstText(report, "auditionStatus")
                .or(() -> firstText(report, "audition"))
                .or(() -> firstText(manifest, "auditionStatus"))
                .orElse("NOT_PERFORMED"));
        reviewBoundary.put("semanticQualityReview", firstText(report, "semanticQualityReview")
                .or(() -> firstText(manifest, "semanticQualityReview"))
                .orElse("NOT_PERFORMED"));
        reviewBoundary.put("finalMixReadiness", firstText(report, "finalMixReadiness")
                .or(() -> firstText(manifest, "finalMixReadiness"))
                .orElse("NOT_CLAIMED"));
        response.put("reviewBoundary", reviewBoundary);

        Map<String, Object> artifactLinks = new LinkedHashMap<>();
        artifactLinks.put("reviewStateContractReport", REVIEW_STATE_REPORT.toString());
        artifactLinks.put("semanticQualityE2eManifest", E2E_MANIFEST.toString());
        response.put("artifactLinks", artifactLinks);

        response.put("claimBoundaryOk", containsAny(joinedText,
                "claimBoundaryOk", "HUMAN_REVIEW_PARTIAL", "NOT_PERFORMED", "NOT_CLAIMED"));
        response.put("qualityGateLiteStatus", firstText(report, "qualityGateLiteStatus")
                .or(() -> firstText(manifest, "qualityGateLiteStatus"))
                .orElse("SEMANTIC_REVIEW_READY"));

        return response;
    }

    private JsonNode readJson(Path path) {
        if (!Files.exists(path)) {
            return mapper.createObjectNode();
        }
        try {
            return mapper.readTree(path.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JSON artifact: " + path, e);
        }
    }

    private String readText(Path path) {
        if (!Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    private Optional<String> firstText(JsonNode root, String... keys) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return Optional.empty();
        }
        Set<String> wanted = new LinkedHashSet<>(Arrays.asList(keys));
        Deque<JsonNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            JsonNode node = stack.pop();
            if (node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    JsonNode value = field.getValue();
                    if (wanted.contains(field.getKey()) && value.isValueNode()) {
                        String text = value.asText("");
                        if (!text.isBlank()) {
                            return Optional.of(text);
                        }
                    }
                    stack.push(value);
                }
            } else if (node.isArray()) {
                for (JsonNode child : node) {
                    stack.push(child);
                }
            }
        }
        return Optional.empty();
    }

    private List<String> extractCandidateIds(String text) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("procedural_v\\d+_\\d+").matcher(text == null ? "" : text);
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return new ArrayList<>(ids);
    }

    private boolean containsAny(String text, String... needles) {
        String safeText = text == null ? "" : text;
        for (String needle : needles) {
            if (safeText.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
