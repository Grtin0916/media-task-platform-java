package com.ryan.media.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RankerRegistryTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sameVersionAndDigestIsIdempotent() {
        RankerRegistry registry = new RankerRegistry(new RankerRegistryRepository());
        RankerBundleManifest manifest = manifest("v1", "a".repeat(64));
        assertFalse(registry.register(manifest, Path.of("stored")).reused());
        assertTrue(registry.register(manifest, Path.of("stored")).reused());
        assertEquals(1, registry.list().size());
    }

    @Test
    void sameVersionAndDifferentDigestConflicts() {
        RankerRegistry registry = new RankerRegistry(new RankerRegistryRepository());
        registry.register(manifest("v1", "a".repeat(64)), Path.of("stored-a"));
        RankerException exception = assertThrows(
                RankerException.class,
                () -> registry.register(manifest("v1", "b".repeat(64)), Path.of("stored-b")));
        assertEquals("RANKER_VERSION_CONFLICT", exception.code());
    }

    private RankerBundleManifest manifest(String version, String digest) {
        return new RankerBundleManifest(
                "ranker-delivery-bundle/v1", "preference-ranker", version,
                RankerPromotionStatus.DATA_BLOCKED, digest, false, false, 0,
                "preference-features-v1", "1".repeat(64), "2".repeat(64),
                "abc", "abc", 0, false, 0, "blocked",
                "ACTIVE_LEARNING_BLOCKED", List.of(), mapper.valueToTree(
                        java.util.Map.of("autoFinalForbidden", true)));
    }
}
