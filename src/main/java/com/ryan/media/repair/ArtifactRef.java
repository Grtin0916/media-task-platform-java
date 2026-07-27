package com.ryan.media.repair;

public record ArtifactRef(
        String sourceRepository,
        String sourceCommit,
        String sourceRelativePath,
        String resolvedSourcePath,
        String materializedPath,
        String sha256,
        long sizeBytes,
        String mediaType,
        boolean exists,
        String integrityStatus) {
}
