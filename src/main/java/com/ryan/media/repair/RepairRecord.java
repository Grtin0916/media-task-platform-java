package com.ryan.media.repair;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RepairRecord {
    private final String repairId;
    private final String parentCandidateId;
    private final String failureId;
    private final String repairAction;
    private final String sourceMode;
    private final JsonNode metrics;
    private final String reason;
    private final ArtifactRef beforeArtifact;
    private final ArtifactRef afterArtifact;
    private final String importBatchId;
    private final String sourceCommit;
    private final Instant createdAt;
    private final List<ReviewEvent> history = new ArrayList<>();
    private RepairWorkflowState workflowState;
    private RepairDecision repairDecision;
    private String reviewStatus;
    private long reviewVersion;
    private Instant updatedAt;
    private ManualReview manualReview;

    public RepairRecord(
            String repairId,
            String parentCandidateId,
            String failureId,
            String repairAction,
            String sourceMode,
            JsonNode metrics,
            String reason,
            ArtifactRef beforeArtifact,
            ArtifactRef afterArtifact,
            String importBatchId,
            String sourceCommit,
            RepairDecision repairDecision) {
        this.repairId = repairId;
        this.parentCandidateId = parentCandidateId;
        this.failureId = failureId;
        this.repairAction = repairAction;
        this.sourceMode = sourceMode;
        this.metrics = metrics.deepCopy();
        this.reason = reason;
        this.beforeArtifact = beforeArtifact;
        this.afterArtifact = afterArtifact;
        this.importBatchId = importBatchId;
        this.sourceCommit = sourceCommit;
        this.repairDecision = repairDecision;
        this.workflowState = repairDecision == RepairDecision.MANUAL_REVIEW
                ? RepairWorkflowState.REVIEW_PENDING
                : RepairWorkflowState.REVIEW_COMPLETED;
        this.reviewStatus = repairDecision == RepairDecision.MANUAL_REVIEW ? "PENDING" : "NOT_REQUIRED";
        this.reviewVersion = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
        this.manualReview = null;
        history.add(new ReviewEvent(0L, repairDecision, workflowState, "imported upstream decision", createdAt));
    }

    public synchronized void submitReview(RepairReviewRequest request) {
        if (request.reviewVersion() == null || request.reviewVersion() != reviewVersion) {
            throw new ReviewVersionConflictException(repairId, reviewVersion);
        }
        if (repairDecision == RepairDecision.REPAIR_REJECTED
                || repairDecision == RepairDecision.REPAIR_BLOCKED) {
            throw new InvalidRepairTransitionException(
                    repairId, repairDecision, "rejected or blocked records cannot be promoted");
        }
        validateCompleteReview(request);
        if (request.targetDecision() == null) {
            throw new InvalidRepairRequestException("targetDecision is required");
        }
        if (request.targetDecision() == RepairDecision.FINAL_SELECTED
                || request.targetDecision() == RepairDecision.RUNNER_UP) {
            if ("unknown".equalsIgnoreCase(request.forbiddenEventStatus())) {
                throw new InvalidRepairRequestException(
                        "forbiddenEventStatus must be absent or present before promotion");
            }
            if ("present".equalsIgnoreCase(request.forbiddenEventStatus())) {
                throw new InvalidRepairTransitionException(
                        repairId, repairDecision, "forbidden event is present");
            }
        }
        repairDecision = request.targetDecision();
        workflowState = RepairWorkflowState.REVIEW_COMPLETED;
        reviewStatus = "COMPLETED";
        reviewVersion++;
        updatedAt = Instant.now();
        manualReview = new ManualReview(
                request.preference(), request.reason(), request.confidence(),
                request.audibleArtifact(), request.forbiddenEventStatus(),
                request.reviewedBy(), updatedAt);
        history.add(new ReviewEvent(
                reviewVersion, repairDecision, workflowState, request.reason(), updatedAt));
    }

    private static void validateCompleteReview(RepairReviewRequest request) {
        if (blank(request.preference())
                || blank(request.reason())
                || request.confidence() == null
                || request.confidence() < 0.0
                || request.confidence() > 1.0
                || blank(request.reviewedBy())
                || blank(request.forbiddenEventStatus())) {
            throw new InvalidRepairRequestException(
                    "preference, reason, confidence[0,1], reviewedBy and forbiddenEventStatus are required");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public String repairId() { return repairId; }
    public String parentCandidateId() { return parentCandidateId; }
    public String failureId() { return failureId; }
    public String repairAction() { return repairAction; }
    public String sourceMode() { return sourceMode; }
    public JsonNode metrics() { return metrics.deepCopy(); }
    public String reason() { return reason; }
    public ArtifactRef beforeArtifact() { return beforeArtifact; }
    public ArtifactRef afterArtifact() { return afterArtifact; }
    public String importBatchId() { return importBatchId; }
    public String sourceCommit() { return sourceCommit; }
    public synchronized RepairWorkflowState workflowState() { return workflowState; }
    public synchronized RepairDecision repairDecision() { return repairDecision; }
    public synchronized String reviewStatus() { return reviewStatus; }
    public synchronized long reviewVersion() { return reviewVersion; }
    public Instant createdAt() { return createdAt; }
    public synchronized Instant updatedAt() { return updatedAt; }
    public synchronized ManualReview manualReview() { return manualReview; }
    public synchronized List<ReviewEvent> history() { return List.copyOf(history); }

    public record ManualReview(
            String preference,
            String reason,
            double confidence,
            String audibleArtifact,
            String forbiddenEventStatus,
            String reviewedBy,
            Instant reviewedAt) {
    }

    public record ReviewEvent(
            long version,
            RepairDecision decision,
            RepairWorkflowState workflowState,
            String reason,
            Instant occurredAt) {
    }
}
