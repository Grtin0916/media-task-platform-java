package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RankerBundleImporterTest {
    @TempDir Path javaRoot;

    @Test
    void importsRealMainbaseBlockedBundleAndReusesDigest() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        RankerRegistry registry = new RankerRegistry(new RankerRegistryRepository());
        RankerEvidenceWriter writer = new RankerEvidenceWriter(mapper, registry, javaRoot.toString());
        RankerBundleImporter importer = new RankerBundleImporter(
                mapper, new RankerArtifactVerifier(mapper), registry, writer,
                new RecommendationHistory(),
                "../audio_engineering_repo_skeleton_v1", javaRoot.toString());
        RankerRegistry.ImportResult first = importer.importBundle(
                "artifacts/models/preference_ranker_v1/delivery");
        RankerRegistry.ImportResult second = importer.importBundle(
                "artifacts/models/preference_ranker_v1/delivery");
        assertEquals(RankerPromotionStatus.DATA_BLOCKED, first.version().promotionStatus());
        assertFalse(first.version().modelPresent());
        assertFalse(first.version().oofAvailable());
        assertEquals(0, first.version().recommendationCount());
        assertFalse(first.reused());
        assertTrue(second.reused());
        assertTrue(Files.isRegularFile(javaRoot.resolve(
                first.version().storedBundlePath()).resolve("manifest.json")));
    }

    @Test
    void blockedInvariantRejectsLearnedOutputs() {
        ObjectMapper mapper = new ObjectMapper();
        RankerBundleManifest invalid = new RankerBundleManifest(
                "ranker-delivery-bundle/v1", "preference-ranker", "bad",
                RankerPromotionStatus.DATA_BLOCKED, "a".repeat(64), true, false, 0,
                "preference-features-v1", "1".repeat(64), "2".repeat(64),
                "abc", "abc", 0, false, 0, "blocked", "BLOCKED",
                java.util.List.of(), mapper.valueToTree(
                        java.util.Map.of("autoFinalForbidden", true)));
        assertEquals(
                "INVALID_BLOCKED_BUNDLE",
                org.junit.jupiter.api.Assertions.assertThrows(
                        RankerException.class,
                        () -> RankerBundleImporter.validateInvariant(invalid)).code());
    }

    @Test
    void candidateImportsPrecomputedRecommendationIntoHistory() throws Exception {
        Path mainbase = javaRoot.resolve("mainbase");
        RankerTestBundleFactory.create(
                mainbase.resolve("candidate"), "candidate-v1", "e".repeat(64),
                RankerPromotionStatus.CANDIDATE, true, true, 1);
        Path output = javaRoot.resolve("java");
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        RankerRegistry registry = new RankerRegistry(new RankerRegistryRepository());
        RecommendationHistory history = new RecommendationHistory();
        RankerBundleImporter importer = new RankerBundleImporter(
                mapper,
                new RankerArtifactVerifier(mapper),
                registry,
                new RankerEvidenceWriter(mapper, registry, output.toString()),
                history,
                mainbase.toString(),
                output.toString());
        importer.importBundle("candidate");
        assertEquals(1, history.forCase("c1").size());
        assertEquals(
                RankerRecommendationStatus.NEEDS_HUMAN_REVIEW,
                history.forCase("c1").get(0).recommendationStatus());
        assertFalse(Files.notExists(output.resolve(
                "artifacts/week21/rankers/sha256/" + "e".repeat(64) + "/ranker.json")));
    }
}
