package com.ryan.media.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RankerBundleImporter {
    private final ObjectMapper mapper;
    private final RankerArtifactVerifier verifier;
    private final RankerRegistry registry;
    private final RankerEvidenceWriter evidenceWriter;
    private final RecommendationHistory recommendationHistory;
    private final Path mainbaseRoot;
    private final Path javaRoot;

    public RankerBundleImporter(
            ObjectMapper mapper,
            RankerArtifactVerifier verifier,
            RankerRegistry registry,
            RankerEvidenceWriter evidenceWriter,
            RecommendationHistory recommendationHistory,
            @Value("${ranker.mainbase-root:../audio_engineering_repo_skeleton_v1}") String mainbaseRoot,
            @Value("${ranker.java-root:.}") String javaRoot) {
        this.mapper = mapper;
        this.verifier = verifier;
        this.registry = registry;
        this.evidenceWriter = evidenceWriter;
        this.recommendationHistory = recommendationHistory;
        this.mainbaseRoot = Path.of(mainbaseRoot).toAbsolutePath().normalize();
        this.javaRoot = Path.of(javaRoot).toAbsolutePath().normalize();
    }

    public RankerRegistry.ImportResult importBundle(String relativePath) {
        Path source = resolveBundle(relativePath);
        RankerBundleManifest manifest = readManifest(source);
        validateInvariant(manifest);
        verifier.verify(source, manifest);
        List<RankerRecommendation> recommendations = readRecommendations(source, manifest);
        Path stored = materialize(source, manifest.bundleDigest());
        RankerRegistry.ImportResult result =
                registry.register(manifest, javaRoot.relativize(stored));
        if (!result.reused()) {
            recommendations.forEach(recommendationHistory::append);
        }
        evidenceWriter.record(result);
        return result;
    }

    private RankerBundleManifest readManifest(Path source) {
        try {
            return mapper.readValue(
                    source.resolve("manifest.json").toFile(), RankerBundleManifest.class);
        } catch (IOException exception) {
            throw new RankerException("INVALID_BUNDLE", "manifest.json is invalid");
        }
    }

    static void validateInvariant(RankerBundleManifest manifest) {
        Set<String> members = manifest.artifacts().stream()
                .map(RankerArtifactRef::relativePath)
                .collect(Collectors.toSet());
        if (manifest.finalSelectedMutationCount() != 0
                || !manifest.claimBoundary().path("autoFinalForbidden").asBoolean(false)) {
            throw new RankerException("PUBLISH_DECISION_MUTATION_FORBIDDEN", "final selection must remain untouched");
        }
        if (manifest.promotionStatus() == RankerPromotionStatus.DATA_BLOCKED) {
            if (manifest.modelPresent() || manifest.oofAvailable()
                    || manifest.recommendationCount() != 0
                    || members.contains("ranker.json")
                    || members.contains("oof-predictions.csv")
                    || members.contains("recommendations.csv")
                    || members.contains("explanations.csv")) {
                throw new RankerException("INVALID_BLOCKED_BUNDLE", "blocked bundle contains learned outputs");
            }
            return;
        }
        if (!manifest.modelPresent()) {
            throw new RankerException("MODEL_ARTIFACT_REQUIRED", "promoted bundle requires a model");
        }
        if (!manifest.oofAvailable()) {
            throw new RankerException("OOF_ARTIFACT_REQUIRED", "promoted bundle requires real OOF");
        }
        if (!members.contains("ranker.json") || !members.contains("oof-predictions.csv")) {
            throw new RankerException(
                    "MODEL_ARTIFACT_REQUIRED", "promoted bundle does not declare model and OOF files");
        }
        if (manifest.promotionStatus() == RankerPromotionStatus.CANDIDATE
                && (manifest.recommendationCount() < 1
                || !members.contains("recommendations.csv"))) {
            throw new RankerException("RECOMMENDATION_REQUIRED", "candidate bundle requires recommendations");
        }
    }

    private List<RankerRecommendation> readRecommendations(
            Path source, RankerBundleManifest manifest) {
        if (manifest.recommendationCount() == 0) return List.of();
        Path csv = source.resolve("recommendations.csv");
        try {
            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            if (lines.isEmpty()) throw new RankerException(
                    "INVALID_RECOMMENDATIONS", "recommendations header is missing");
            String[] header = lines.get(0).split(",", -1);
            List<RankerRecommendation> results = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                if (lines.get(index).isBlank()) continue;
                String[] values = lines.get(index).split(",", -1);
                String caseId = value(header, values, "case_id");
                String candidate = value(header, values, "candidate");
                String statusValue = optional(header, values, "recommendation_status",
                        "NEEDS_HUMAN_REVIEW");
                RankerRecommendationStatus status;
                try { status = RankerRecommendationStatus.valueOf(statusValue); }
                catch (IllegalArgumentException exception) {
                    throw new RankerException(
                            "INVALID_RECOMMENDATIONS", "unknown recommendation status: " + statusValue);
                }
                List<RankerRecommendation> previous = recommendationHistory.forCase(caseId);
                String supersedes = previous.isEmpty()
                        ? null : previous.get(previous.size() - 1).recommendationId();
                String identity = manifest.rankerVersion() + "|" + caseId + "|" + candidate;
                results.add(new RankerRecommendation(
                        UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString(),
                        caseId,
                        manifest.rankerVersion(),
                        manifest.bundleDigest(),
                        manifest.featureSnapshotDigest(),
                        candidate,
                        decimal(header, values, "score"),
                        decimal(header, values, "margin"),
                        status,
                        optional(header, values, "reason", ""),
                        Instant.now(),
                        supersedes));
            }
            if (results.size() != manifest.recommendationCount()) {
                throw new RankerException(
                        "INVALID_RECOMMENDATIONS",
                        "recommendationCount does not match recommendations.csv");
            }
            return List.copyOf(results);
        } catch (IOException exception) {
            throw new RankerException("INVALID_RECOMMENDATIONS", "cannot read recommendations.csv");
        }
    }

    private static String value(String[] header, String[] row, String field) {
        String result = optional(header, row, field, "");
        if (result.isBlank()) {
            throw new RankerException("INVALID_RECOMMENDATIONS", "missing " + field);
        }
        return result;
    }

    private static String optional(
            String[] header, String[] row, String field, String fallback) {
        for (int index = 0; index < header.length; index++) {
            if (field.equals(header[index]) && index < row.length) return row[index];
        }
        return fallback;
    }

    private static Double decimal(String[] header, String[] row, String field) {
        String value = optional(header, row, field, "");
        if (value.isBlank()) return null;
        try { return Double.valueOf(value); }
        catch (NumberFormatException exception) {
            throw new RankerException("INVALID_RECOMMENDATIONS", "invalid " + field);
        }
    }

    private Path resolveBundle(String relativePath) {
        if (relativePath == null || relativePath.isBlank() || Path.of(relativePath).isAbsolute()) {
            throw new RankerException("RANKER_PATH_OUTSIDE_ALLOWED_ROOT", "relative bundle path required");
        }
        Path candidate = mainbaseRoot.resolve(relativePath).normalize();
        if (!candidate.startsWith(mainbaseRoot) || relativePath.contains("..")) {
            throw new RankerException("RANKER_PATH_OUTSIDE_ALLOWED_ROOT", relativePath);
        }
        try {
            Path realRoot = mainbaseRoot.toRealPath();
            Path real = candidate.toRealPath();
            if (!real.startsWith(realRoot)
                    || !Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(candidate)) {
                throw new RankerException("RANKER_PATH_OUTSIDE_ALLOWED_ROOT", relativePath);
            }
            return real;
        } catch (IOException exception) {
            throw new RankerException("RANKER_BUNDLE_MISSING", relativePath);
        }
    }

    private Path materialize(Path source, String digest) {
        Path target = javaRoot.resolve("artifacts/week21/rankers/sha256")
                .resolve(digest).normalize();
        if (!target.startsWith(javaRoot)) {
            throw new RankerException("RANKER_PATH_OUTSIDE_ALLOWED_ROOT", digest);
        }
        try {
            Files.createDirectories(target);
            try (var files = Files.list(source)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    Path destination = target.resolve(file.getFileName().toString());
                    if (!Files.exists(destination)) {
                        Files.copy(file, destination, StandardCopyOption.COPY_ATTRIBUTES);
                    } else if (!RankerArtifactVerifier.sha256(file)
                            .equals(RankerArtifactVerifier.sha256(destination))) {
                        throw new RankerException(
                                "RANKER_ARTIFACT_DIGEST_MISMATCH",
                                "content store collision: " + file.getFileName());
                    }
                }
            }
            return target;
        } catch (IOException exception) {
            throw new RankerException("RANKER_ARTIFACT_COPY_FAILED", exception.getMessage());
        }
    }
}
