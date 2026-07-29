package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RankerArtifactVerifierTest {
    @TempDir Path temporary;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void verifiesCompleteChecksummedBundle() throws Exception {
        Path bundle = RankerTestBundleFactory.create(
                temporary.resolve("ok"), "v1", "a".repeat(64),
                RankerPromotionStatus.DATA_BLOCKED, false, false, 0);
        RankerBundleManifest manifest =
                mapper.readValue(bundle.resolve("manifest.json").toFile(), RankerBundleManifest.class);
        assertDoesNotThrow(() -> new RankerArtifactVerifier(mapper).verify(bundle, manifest));
    }

    @Test
    void rejectsDigestMismatch() throws Exception {
        Path bundle = RankerTestBundleFactory.create(
                temporary.resolve("bad"), "v1", "b".repeat(64),
                RankerPromotionStatus.DATA_BLOCKED, false, false, 0);
        RankerBundleManifest manifest =
                mapper.readValue(bundle.resolve("manifest.json").toFile(), RankerBundleManifest.class);
        Files.writeString(bundle.resolve("model-card.json"), "tampered");
        RankerException exception = assertThrows(
                RankerException.class,
                () -> new RankerArtifactVerifier(mapper).verify(bundle, manifest));
        assertEquals("RANKER_ARTIFACT_DIGEST_MISMATCH", exception.code());
    }

    @Test
    void rejectsTraversal() {
        RankerArtifactVerifier verifier = new RankerArtifactVerifier(mapper);
        RankerException exception = assertThrows(
                RankerException.class, () -> verifier.resolve(temporary, "../secret"));
        assertEquals("RANKER_PATH_OUTSIDE_ALLOWED_ROOT", exception.code());
    }
}
