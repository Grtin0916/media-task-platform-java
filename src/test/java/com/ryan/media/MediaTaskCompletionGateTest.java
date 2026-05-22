package com.ryan.media;

import com.ryan.media.model.MediaTaskCompletionGate;
import com.ryan.media.model.MediaTaskEvidenceLinks;
import com.ryan.media.model.MediaTaskLifecycleState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaTaskCompletionGateTest {

    @Test
    void succeededTaskShouldRequireCompleteEvidenceLinks() {
        MediaTaskEvidenceLinks links = new MediaTaskEvidenceLinks(
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertTrue(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.SUCCEEDED,
                links
        ));

        assertDoesNotThrow(() -> MediaTaskCompletionGate.requireCompletedEvidence(
                MediaTaskLifecycleState.SUCCEEDED,
                links
        ));
    }

    @Test
    void succeededTaskShouldRejectMissingArtifactUri() {
        MediaTaskEvidenceLinks links = new MediaTaskEvidenceLinks(
                "",
                "artifacts/evals/week11_eval_v0_metrics.json",
                "CREATED"
        );

        assertFalse(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.SUCCEEDED,
                links
        ));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> MediaTaskCompletionGate.requireCompletedEvidence(
                        MediaTaskLifecycleState.SUCCEEDED,
                        links
                )
        );

        assertTrue(error.getMessage().contains("artifactUri"));
        assertTrue(error.getMessage().contains("evalSummaryUri"));
        assertTrue(error.getMessage().contains("qualityGateStatus"));
    }

    @Test
    void succeededTaskShouldRejectMissingEvalSummaryUri() {
        MediaTaskEvidenceLinks links = new MediaTaskEvidenceLinks(
                "artifacts/evals/week11_eval_v0.json",
                " ",
                "CREATED"
        );

        assertFalse(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.SUCCEEDED,
                links
        ));
    }

    @Test
    void succeededTaskShouldRejectMissingQualityGateStatus() {
        MediaTaskEvidenceLinks links = new MediaTaskEvidenceLinks(
                "artifacts/evals/week11_eval_v0.json",
                "artifacts/evals/week11_eval_v0_metrics.json",
                null
        );

        assertFalse(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.SUCCEEDED,
                links
        ));
    }

    @Test
    void nonSucceededStatesShouldNotRequireEvidenceLinksYet() {
        assertTrue(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.CREATED,
                null
        ));
        assertTrue(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.QUEUED,
                null
        ));
        assertTrue(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.RUNNING,
                null
        ));
        assertTrue(MediaTaskCompletionGate.canExposeAsCompleted(
                MediaTaskLifecycleState.FAILED,
                null
        ));
    }
}