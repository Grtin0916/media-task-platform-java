package com.ryan.media.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RankerTestBundleFactory {
    private RankerTestBundleFactory() {
    }

    static Path create(
            Path root,
            String version,
            String digest,
            RankerPromotionStatus status,
            boolean modelPresent,
            boolean oofAvailable,
            int recommendationCount) throws IOException {
        Files.createDirectories(root);
        ObjectMapper mapper = new ObjectMapper();
        write(mapper, root.resolve("model-card.json"), Map.of(
                "promotionStatus", status,
                "trainingPerformed", status != RankerPromotionStatus.DATA_BLOCKED));
        write(mapper, root.resolve("feature-schema.json"), Map.of(
                "schemaVersion", "preference-features-v1",
                "featureNames", List.of("event_coverage")));
        write(mapper, root.resolve("claim-boundary.json"), Map.of(
                "proxyOnly", true,
                "humanGateRequired", true,
                "autoFinalForbidden", true,
                "finalSelectedMutationCount", 0));
        if (modelPresent) Files.writeString(root.resolve("ranker.json"), "{\"weights\":[1.0]}\n");
        if (oofAvailable) Files.writeString(root.resolve("oof-predictions.csv"), "case_id,score\nc1,0.5\n");
        if (recommendationCount > 0) {
            Files.writeString(root.resolve("recommendations.csv"), "case_id,candidate\nc1,A\n");
        }

        List<Map<String, Object>> artifacts = new ArrayList<>();
        try (var files = Files.list(root)) {
            for (Path file : files.sorted().toList()) {
                artifacts.add(new LinkedHashMap<>(Map.of(
                        "relativePath", file.getFileName().toString(),
                        "sha256", RankerArtifactVerifier.sha256(file),
                        "sizeBytes", Files.size(file),
                        "mediaType", file.toString().endsWith(".json")
                                ? "application/json" : "text/csv",
                        "requiredForStatus", true)));
            }
        }
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schemaVersion", "ranker-delivery-bundle/v1");
        manifest.put("rankerName", "preference-ranker");
        manifest.put("rankerVersion", version);
        manifest.put("promotionStatus", status);
        manifest.put("bundleDigest", digest);
        manifest.put("modelPresent", modelPresent);
        manifest.put("oofAvailable", oofAvailable);
        manifest.put("recommendationCount", recommendationCount);
        manifest.put("featureSchemaVersion", "preference-features-v1");
        manifest.put("featureSnapshotDigest", "1".repeat(64));
        manifest.put("trainingDatasetDigest", "2".repeat(64));
        manifest.put("trainingCodeCommit", "abc123");
        manifest.put("sourceGitHead", "abc123");
        manifest.put("reviewSubmittedCount", status == RankerPromotionStatus.DATA_BLOCKED ? 0 : 48);
        manifest.put("humanReviewCompleted", status != RankerPromotionStatus.DATA_BLOCKED);
        manifest.put("finalSelectedMutationCount", 0);
        manifest.put("blockedReason", status == RankerPromotionStatus.DATA_BLOCKED
                ? "human labels unavailable" : null);
        manifest.put("activeLearningStatus", status == RankerPromotionStatus.DATA_BLOCKED
                ? "ACTIVE_LEARNING_BLOCKED" : "READY");
        manifest.put("artifacts", artifacts);
        manifest.put("claimBoundary", Map.of(
                "proxyOnly", true,
                "humanGateRequired", true,
                "autoFinalForbidden", true));
        write(mapper, root.resolve("manifest.json"), manifest);
        rewriteChecksums(root);
        return root;
    }

    static void rewriteChecksums(Path root) throws IOException {
        StringBuilder checksums = new StringBuilder();
        try (var files = Files.list(root)) {
            for (Path file : files
                    .filter(path -> !path.getFileName().toString().equals("checksums.sha256"))
                    .sorted()
                    .toList()) {
                checksums.append(RankerArtifactVerifier.sha256(file))
                        .append("  ").append(file.getFileName()).append('\n');
            }
        }
        Files.writeString(root.resolve("checksums.sha256"), checksums);
    }

    private static void write(ObjectMapper mapper, Path path, Object value) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
    }
}
