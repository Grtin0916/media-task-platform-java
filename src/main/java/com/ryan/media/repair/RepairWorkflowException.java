package com.ryan.media.repair;

abstract class RepairWorkflowException extends RuntimeException {
    RepairWorkflowException(String message) {
        super(message);
    }
}

final class InvalidRepairRequestException extends RepairWorkflowException {
    InvalidRepairRequestException(String message) {
        super(message);
    }
}

final class ArtifactImportException extends RepairWorkflowException {
    private final String artifactPath;

    ArtifactImportException(String message, String artifactPath) {
        super(message);
        this.artifactPath = artifactPath;
    }

    String artifactPath() {
        return artifactPath;
    }
}

final class InvalidRepairTransitionException extends RepairWorkflowException {
    private final String repairId;
    private final RepairDecision currentDecision;

    InvalidRepairTransitionException(
            String repairId, RepairDecision currentDecision, String message) {
        super(message);
        this.repairId = repairId;
        this.currentDecision = currentDecision;
    }

    String repairId() { return repairId; }
    RepairDecision currentDecision() { return currentDecision; }
}

final class ReviewVersionConflictException extends RepairWorkflowException {
    private final String repairId;
    private final long currentVersion;

    ReviewVersionConflictException(String repairId, long currentVersion) {
        super("stale reviewVersion");
        this.repairId = repairId;
        this.currentVersion = currentVersion;
    }

    String repairId() { return repairId; }
    long currentVersion() { return currentVersion; }
}

final class RepairNotFoundException extends RepairWorkflowException {
    RepairNotFoundException(String repairId) {
        super("repair record not found: " + repairId);
    }
}
