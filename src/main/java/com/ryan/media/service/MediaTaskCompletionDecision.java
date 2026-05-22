package com.ryan.media.service;

import com.ryan.media.model.MediaTaskLifecycleState;

/**
 * Week12 service/API boundary decision for media-task completion exposure.
 *
 * Boundary:
 * - This is not persistence.
 * - This is not a worker runtime.
 * - This is not production artifact registry validation.
 */
public record MediaTaskCompletionDecision(
        MediaTaskLifecycleState state,
        boolean terminal,
        boolean requiresEvidenceLinks,
        boolean canExposeAsCompleted,
        String reason
) {
}