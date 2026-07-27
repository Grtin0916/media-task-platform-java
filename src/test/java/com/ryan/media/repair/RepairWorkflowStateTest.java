package com.ryan.media.repair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class RepairWorkflowStateTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectedRecordCannotBecomeFinalSelected() {
        RepairRecord record = record(RepairDecision.REPAIR_REJECTED);
        assertThrows(
                InvalidRepairTransitionException.class,
                () -> record.submitReview(completeReview(0L, RepairDecision.FINAL_SELECTED)));
    }

    @Test
    void blockedRecordCannotBecomeFinalSelected() {
        RepairRecord record = record(RepairDecision.REPAIR_BLOCKED);
        assertThrows(
                InvalidRepairTransitionException.class,
                () -> record.submitReview(completeReview(0L, RepairDecision.FINAL_SELECTED)));
    }

    @Test
    void incompleteManualReviewCannotPromote() {
        RepairRecord record = record(RepairDecision.MANUAL_REVIEW);
        RepairReviewRequest incomplete = new RepairReviewRequest(
                "A", "", 0.9, "none", "absent", "reviewer",
                RepairDecision.FINAL_SELECTED, 0L);
        assertThrows(
                InvalidRepairRequestException.class,
                () -> record.submitReview(incomplete));
    }

    @Test
    void staleReviewVersionIsRejected() {
        RepairRecord record = record(RepairDecision.MANUAL_REVIEW);
        assertThrows(
                ReviewVersionConflictException.class,
                () -> record.submitReview(completeReview(5L, RepairDecision.RUNNER_UP)));
    }

    @Test
    void completeReviewCanKeepManualReviewWithoutInventingWinner() {
        RepairRecord record = record(RepairDecision.MANUAL_REVIEW);
        record.submitReview(completeReview(0L, RepairDecision.MANUAL_REVIEW));
        assertEquals(RepairDecision.MANUAL_REVIEW, record.repairDecision());
        assertEquals(RepairWorkflowState.REVIEW_COMPLETED, record.workflowState());
        assertEquals(1L, record.reviewVersion());
    }

    private RepairRecord record(RepairDecision decision) {
        ArtifactRef artifact = new ArtifactRef(
                "repo", "commit", "source.wav", "/source.wav",
                "artifacts/blob.wav", "abc", 3, "audio/wav", true, "SHA256_VERIFIED");
        return new RepairRecord(
                "repair-1", "candidate-1", "failure-1", "action", "mixed_only",
                objectMapper.createObjectNode(), "reason", artifact, artifact,
                "batch", "commit", decision);
    }

    private static RepairReviewRequest completeReview(
            long version, RepairDecision targetDecision) {
        return new RepairReviewRequest(
                "A", "clearer event", 0.8, "none", "absent", "human-reviewer",
                targetDecision, version);
    }
}
