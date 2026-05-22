package com.ryan.media.model;

import java.util.Objects;

/**
 * Week12 completion gate that binds lifecycle success to evidence-link completeness.
 *
 * Rule:
 * - SUCCEEDED requires artifactUri + evalSummaryUri + qualityGateStatus.
 * - CREATED / QUEUED / RUNNING / FAILED do not require successful evidence links.
 *
 * This intentionally avoids persistence, queueing, retry, worker orchestration, or artifact registry claims.
 */
public final class MediaTaskCompletionGate {

    private MediaTaskCompletionGate() {
    }

    public static boolean canExposeAsCompleted(
            MediaTaskLifecycleState state,
            MediaTaskEvidenceLinks evidenceLinks
    ) {
        Objects.requireNonNull(state, "state must not be null");

        if (!MediaTaskLifecycleTransition.requiresEvidenceLinks(state)) {
            return true;
        }

        return evidenceLinks != null && evidenceLinks.isComplete();
    }

    public static void requireCompletedEvidence(
            MediaTaskLifecycleState state,
            MediaTaskEvidenceLinks evidenceLinks
    ) {
        if (!canExposeAsCompleted(state, evidenceLinks)) {
            throw new IllegalStateException(
                    "Media task state " + state + " requires complete evidence links: "
                            + "artifactUri, evalSummaryUri, qualityGateStatus"
            );
        }
    }
}