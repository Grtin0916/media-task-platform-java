package com.ryan.media.demo;
public record DemoArtifactRef(
        String sourceCommit, String sourceRelativePath, String sourceDigest,
        String materializedPath, long sizeBytes, String mediaType, String integrityStatus) {}
