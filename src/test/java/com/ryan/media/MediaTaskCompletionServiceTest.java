package com.ryan.media;

import com.ryan.media.model.MediaTaskLifecycleState;
import com.ryan.media.service.MediaTaskCompletionDecision;
import com.ryan.media.service.MediaTaskCompletionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTaskCompletionServiceTest {

    private final MediaTaskCompletionService service = new MediaTaskCompletionService();

    @Test
    void succeededWithCompleteEvidenceShouldBeExposedAsCompleted() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "SUCCEEDED",
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertEquals(MediaTaskLifecycleState.SUCCEEDED, decision.state());
        assertTrue(decision.terminal());
        assertTrue(decision.requiresEvidenceLinks());
        assertTrue(decision.canExposeAsCompleted());
        assertEquals("SUCCEEDED with complete evidence links", decision.reason());
    }

    @Test
    void succeededShouldAcceptLowercaseStatusAfterNormalization() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                " succeeded ",
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertEquals(MediaTaskLifecycleState.SUCCEEDED, decision.state());
        assertTrue(decision.canExposeAsCompleted());
    }

    @Test
    void succeededWithoutArtifactShouldBeRejected() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "SUCCEEDED",
                "",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertEquals(MediaTaskLifecycleState.SUCCEEDED, decision.state());
        assertTrue(decision.terminal());
        assertTrue(decision.requiresEvidenceLinks());
        assertFalse(decision.canExposeAsCompleted());
        assertTrue(decision.reason().contains("artifactUri"));
    }

    @Test
    void succeededWithoutEvalSummaryShouldBeRejected() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "SUCCEEDED",
                "artifacts/evals/week11_eval_v0.json",
                " ",
                "CREATED"
        );

        assertEquals(MediaTaskLifecycleState.SUCCEEDED, decision.state());
        assertFalse(decision.canExposeAsCompleted());
    }

    @Test
    void succeededWithoutQualityGateStatusShouldBeRejected() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "SUCCEEDED",
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                null
        );

        assertEquals(MediaTaskLifecycleState.SUCCEEDED, decision.state());
        assertFalse(decision.canExposeAsCompleted());
    }

    @Test
    void runningShouldNotBeExposedAsCompletedEvenWithEvidenceLinks() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "RUNNING",
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertEquals(MediaTaskLifecycleState.RUNNING, decision.state());
        assertFalse(decision.terminal());
        assertFalse(decision.requiresEvidenceLinks());
        assertFalse(decision.canExposeAsCompleted());
        assertEquals("state is not SUCCEEDED", decision.reason());
    }

    @Test
    void failedShouldBeTerminalButNotCompleted() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "FAILED",
                null,
                null,
                null
        );

        assertEquals(MediaTaskLifecycleState.FAILED, decision.state());
        assertTrue(decision.terminal());
        assertFalse(decision.requiresEvidenceLinks());
        assertFalse(decision.canExposeAsCompleted());
    }

    @Test
    void unknownStatusShouldBeRejectedConservatively() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                "DONE",
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertNull(decision.state());
        assertFalse(decision.terminal());
        assertFalse(decision.requiresEvidenceLinks());
        assertFalse(decision.canExposeAsCompleted());
        assertEquals("unknown or blank lifecycle status", decision.reason());
    }

    @Test
    void blankStatusShouldBeRejectedConservatively() {
        MediaTaskCompletionDecision decision = service.decideCompletionExposure(
                " ",
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertNull(decision.state());
        assertFalse(decision.canExposeAsCompleted());
    }
}