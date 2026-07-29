package com.ryan.media.ranking;

public record RankerArtifactRef(
        String relativePath,
        String sha256,
        long sizeBytes,
        String mediaType,
        boolean requiredForStatus) {
}
