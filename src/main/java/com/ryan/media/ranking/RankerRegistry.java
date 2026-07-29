package com.ryan.media.ranking;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RankerRegistry {
    private final RankerRegistryRepository repository;

    public RankerRegistry(RankerRegistryRepository repository) {
        this.repository = repository;
    }

    public synchronized ImportResult register(RankerBundleManifest manifest, Path stored) {
        RankerVersion current = repository.find(manifest.rankerVersion());
        if (current != null) {
            if (!current.bundleDigest().equals(manifest.bundleDigest())) {
                throw new RankerException(
                        "RANKER_VERSION_CONFLICT",
                        "ranker version already has different content",
                        manifest.rankerVersion(),
                        manifest.bundleDigest(),
                        manifest.promotionStatus());
            }
            return new ImportResult(current, true, false);
        }
        boolean duplicateContent = repository.findAll().stream()
                .anyMatch(item -> item.bundleDigest().equals(manifest.bundleDigest()));
        RankerVersion version = new RankerVersion(
                manifest.rankerVersion(),
                manifest.bundleDigest(),
                manifest.promotionStatus(),
                manifest.modelPresent(),
                manifest.oofAvailable(),
                manifest.recommendationCount(),
                manifest.featureSchemaVersion(),
                manifest.featureSnapshotDigest(),
                manifest.reviewSubmittedCount(),
                manifest.humanReviewCompleted(),
                manifest.finalSelectedMutationCount(),
                manifest.blockedReason(),
                stored.toString().replace('\\', '/'),
                Instant.now());
        repository.save(version);
        return new ImportResult(version, false, duplicateContent);
    }

    public List<RankerVersion> list() {
        return repository.findAll();
    }

    public RankerVersion require(String version) {
        RankerVersion found = repository.find(version);
        if (found == null) throw new RankerException("RANKER_VERSION_NOT_FOUND", version);
        return found;
    }

    public RankerVersion latest() {
        List<RankerVersion> versions = list();
        if (versions.isEmpty()) throw new RankerException("RANKER_VERSION_NOT_FOUND", "registry is empty");
        return versions.get(versions.size() - 1);
    }

    public record ImportResult(RankerVersion version, boolean reused, boolean duplicateContent) {
    }
}
