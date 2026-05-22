package com.ryan.media;

import com.ryan.media.model.MediaTaskLifecycleState;
import com.ryan.media.model.MediaTaskLifecycleTransition;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MediaTaskLifecycleTransitionTest {

    @Test
    void createdTaskShouldMoveForwardThroughHappyPath() {
        assertTrue(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.CREATED,
                MediaTaskLifecycleState.QUEUED
        ));
        assertTrue(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.QUEUED,
                MediaTaskLifecycleState.RUNNING
        ));
        assertTrue(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.RUNNING,
                MediaTaskLifecycleState.SUCCEEDED
        ));
    }

    @Test
    void failureShouldBeAllowedFromNonTerminalStates() {
        assertTrue(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.CREATED,
                MediaTaskLifecycleState.FAILED
        ));
        assertTrue(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.QUEUED,
                MediaTaskLifecycleState.FAILED
        ));
        assertTrue(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.RUNNING,
                MediaTaskLifecycleState.FAILED
        ));
    }

    @Test
    void terminalStatesShouldRejectFurtherTransitions() {
        assertTrue(MediaTaskLifecycleTransition.isTerminal(MediaTaskLifecycleState.SUCCEEDED));
        assertTrue(MediaTaskLifecycleTransition.isTerminal(MediaTaskLifecycleState.FAILED));

        assertFalse(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.SUCCEEDED,
                MediaTaskLifecycleState.RUNNING
        ));
        assertFalse(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.FAILED,
                MediaTaskLifecycleState.QUEUED
        ));
    }

    @Test
    void illegalBackwardOrSkipTransitionsShouldBeRejected() {
        assertFalse(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.CREATED,
                MediaTaskLifecycleState.RUNNING
        ));
        assertFalse(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.QUEUED,
                MediaTaskLifecycleState.SUCCEEDED
        ));
        assertFalse(MediaTaskLifecycleTransition.canTransition(
                MediaTaskLifecycleState.RUNNING,
                MediaTaskLifecycleState.CREATED
        ));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> MediaTaskLifecycleTransition.requireTransition(
                        MediaTaskLifecycleState.RUNNING,
                        MediaTaskLifecycleState.CREATED
                )
        );
        assertTrue(error.getMessage().contains("RUNNING -> CREATED"));
    }

    @Test
    void allowedNextStatesShouldBeExplicitAndBounded() {
        assertEquals(
                Set.of(MediaTaskLifecycleState.QUEUED, MediaTaskLifecycleState.FAILED),
                MediaTaskLifecycleTransition.allowedNextStates(MediaTaskLifecycleState.CREATED)
        );
        assertEquals(
                Set.of(MediaTaskLifecycleState.RUNNING, MediaTaskLifecycleState.FAILED),
                MediaTaskLifecycleTransition.allowedNextStates(MediaTaskLifecycleState.QUEUED)
        );
        assertEquals(
                Set.of(MediaTaskLifecycleState.SUCCEEDED, MediaTaskLifecycleState.FAILED),
                MediaTaskLifecycleTransition.allowedNextStates(MediaTaskLifecycleState.RUNNING)
        );
    }

    @Test
    void succeededStateShouldRequireEvidenceLinks() {
        assertTrue(MediaTaskLifecycleTransition.requiresEvidenceLinks(MediaTaskLifecycleState.SUCCEEDED));
        assertFalse(MediaTaskLifecycleTransition.requiresEvidenceLinks(MediaTaskLifecycleState.CREATED));
        assertFalse(MediaTaskLifecycleTransition.requiresEvidenceLinks(MediaTaskLifecycleState.QUEUED));
        assertFalse(MediaTaskLifecycleTransition.requiresEvidenceLinks(MediaTaskLifecycleState.RUNNING));
        assertFalse(MediaTaskLifecycleTransition.requiresEvidenceLinks(MediaTaskLifecycleState.FAILED));
    }
}