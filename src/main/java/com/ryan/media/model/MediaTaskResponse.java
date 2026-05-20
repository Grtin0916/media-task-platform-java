package com.ryan.media.model;

import java.time.Instant;

public record MediaTaskResponse(
        String id,
        String title,
        String mediaType,
        String status,
        Instant createdAt,
        String artifactUri,
        String evalSummaryUri,
        String qualityGateStatus
) {
    public MediaTaskResponse(String id, String title, String mediaType, String status, Instant createdAt) {
        this(id, title, mediaType, status, createdAt, null, null, null);
    }
}
