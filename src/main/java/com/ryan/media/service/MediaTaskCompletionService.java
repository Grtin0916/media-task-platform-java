package com.ryan.media.service;

import com.ryan.media.model.MediaTaskCompletionGate;
import com.ryan.media.model.MediaTaskEvidenceLinks;
import com.ryan.media.model.MediaTaskLifecycleState;
import com.ryan.media.model.MediaTaskLifecycleTransition;

import java.util.Locale;

/**
 * Week12 V0 service adapter that binds external/API status strings to lifecycle + evidence rules.
 *
 * Rule:
 * - Unknown or blank status cannot be exposed as completed.
 * - SUCCEEDED requires artifactUri + evalSummaryUri + qualityGateStatus.
 * - Non-SUCCEEDED states are not exposed as completed.
 * - FAILED is terminal but not completed.
 */
public final class MediaTaskCompletionService {

    public MediaTaskCompletionDecision decideCompletionExposure(
            String rawStatus,
            String artifactUri,
            String evalSummaryUri,
            String qualityGateStatus
    ) {
        MediaTaskLifecycleState state = parseState(rawStatus);
        if (state == null) {
            return new MediaTaskCompletionDecision(
                    null,
                    false,
                    false,
                    false,
                    "unknown or blank lifecycle status"
            );
        }

        boolean terminal = MediaTaskLifecycleTransition.isTerminal(state);
        boolean requiresEvidenceLinks = MediaTaskLifecycleTransition.requiresEvidenceLinks(state);

        if (state != MediaTaskLifecycleState.SUCCEEDED) {
            return new MediaTaskCompletionDecision(
                    state,
                    terminal,
                    requiresEvidenceLinks,
                    false,
                    "state is not SUCCEEDED"
            );
        }

        MediaTaskEvidenceLinks links = new MediaTaskEvidenceLinks(
                artifactUri,
                evalSummaryUri,
                qualityGateStatus
        );

        boolean canExpose = MediaTaskCompletionGate.canExposeAsCompleted(state, links);
        return new MediaTaskCompletionDecision(
                state,
                terminal,
                true,
                canExpose,
                canExpose
                        ? "SUCCEEDED with complete evidence links"
                        : "SUCCEEDED requires artifactUri, evalSummaryUri, qualityGateStatus"
        );
    }

    private MediaTaskLifecycleState parseState(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return null;
        }

        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        try {
            return MediaTaskLifecycleState.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}