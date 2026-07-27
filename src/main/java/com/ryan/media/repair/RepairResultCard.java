package com.ryan.media.repair;

import com.fasterxml.jackson.databind.JsonNode;

public record RepairResultCard(
        String repairId,
        String parentCandidateId,
        String failureId,
        ArtifactRef before,
        ArtifactRef after,
        String repairAction,
        String sourceMode,
        JsonNode metrics,
        RepairWorkflowState workflowState,
        RepairDecision repairDecision,
        String reviewStatus,
        long reviewVersion,
        RepairRecord.ManualReview manualReview,
        String reason,
        String sourceCommit,
        String importBatchId) {

    static RepairResultCard from(RepairRecord record) {
        return new RepairResultCard(
                record.repairId(),
                record.parentCandidateId(),
                record.failureId(),
                record.beforeArtifact(),
                record.afterArtifact(),
                record.repairAction(),
                record.sourceMode(),
                record.metrics(),
                record.workflowState(),
                record.repairDecision(),
                record.reviewStatus(),
                record.reviewVersion(),
                record.manualReview(),
                record.reason(),
                record.sourceCommit(),
                record.importBatchId());
    }
}
