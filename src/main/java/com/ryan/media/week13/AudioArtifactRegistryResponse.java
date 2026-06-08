package com.ryan.media.week13;

import java.util.List;
import java.util.Map;

public record AudioArtifactRegistryResponse(
        String status,
        String scope,
        int candidateCount,
        Map<String, Long> assetTimeModeCounts,
        long placementRequiredCount,
        long readyCount,
        List<String> contractFields,
        String boundaryStatement,
        List<AudioArtifactRegistryItem> items
) {
}
