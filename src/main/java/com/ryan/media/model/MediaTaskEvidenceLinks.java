package com.ryan.media.model;

/**
 * Minimal Week12 evidence-link carrier.
 *
 * Boundary:
 * - This is a local domain contract, not durable artifact storage.
 * - It does not claim signed URL lifecycle, object-store retention, or production SLO.
 */
public record MediaTaskEvidenceLinks(
        String artifactUri,
        String evalSummaryUri,
        String qualityGateStatus
) {
    public boolean isComplete() {
        return hasText(artifactUri) && hasText(evalSummaryUri) && hasText(qualityGateStatus);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}