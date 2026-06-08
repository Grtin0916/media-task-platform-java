package com.ryan.media.week13;

import java.math.BigDecimal;

public record AudioArtifactRegistryItem(
        String candidateId,
        String audioUri,
        String sourceType,
        String caseId,
        String sceneId,
        String eventId,
        String layer,
        String label,
        String assetTimeMode,
        boolean placementRequired,
        BigDecimal expectedStartSec,
        BigDecimal expectedEndSec,
        BigDecimal globalStartSec,
        BigDecimal globalEndSec,
        BigDecimal placementOffsetSec,
        String status
) {
}
