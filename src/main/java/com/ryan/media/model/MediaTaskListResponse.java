package com.ryan.media.model;

import java.util.List;

public record MediaTaskListResponse(
        List<MediaTaskResponse> content,
        int page,
        int size,
        long totalElements,
        String status,
        String sort
) {
}
