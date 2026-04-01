package com.ryan.media.model;

import java.time.Instant;

public record MediaTaskResponse(
        String id,
        String title,
        String mediaType,
        String status,
        Instant createdAt
) {
}
