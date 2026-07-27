package com.ryan.media.repair;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = RepairWorkflowController.class)
public class RepairWorkflowExceptionHandler {
    @ExceptionHandler(ArtifactImportException.class)
    ResponseEntity<ProblemDetail> artifact(ArtifactImportException exception) {
        ProblemDetail detail = base(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "REPAIR_ARTIFACT_IMPORT_REJECTED",
                exception.getMessage());
        detail.setProperty("artifactPath", exception.artifactPath());
        return ResponseEntity.unprocessableEntity().body(detail);
    }

    @ExceptionHandler(InvalidRepairRequestException.class)
    ResponseEntity<ProblemDetail> badRequest(InvalidRepairRequestException exception) {
        return ResponseEntity.badRequest().body(base(
                HttpStatus.BAD_REQUEST,
                "INVALID_REPAIR_REVIEW",
                exception.getMessage()));
    }

    @ExceptionHandler(InvalidRepairTransitionException.class)
    ResponseEntity<ProblemDetail> transition(InvalidRepairTransitionException exception) {
        ProblemDetail detail = base(
                HttpStatus.CONFLICT,
                "INVALID_REPAIR_TRANSITION",
                exception.getMessage());
        detail.setProperty("repairId", exception.repairId());
        detail.setProperty("currentDecision", exception.currentDecision());
        detail.setProperty(
                "allowedTransitions",
                exception.currentDecision() == RepairDecision.MANUAL_REVIEW
                        ? List.of("FINAL_SELECTED", "RUNNER_UP", "REPAIR_REJECTED", "MANUAL_REVIEW")
                        : List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(detail);
    }

    @ExceptionHandler(ReviewVersionConflictException.class)
    ResponseEntity<ProblemDetail> version(ReviewVersionConflictException exception) {
        ProblemDetail detail = base(
                HttpStatus.PRECONDITION_FAILED,
                "STALE_REVIEW_VERSION",
                exception.getMessage());
        detail.setProperty("repairId", exception.repairId());
        detail.setProperty("currentReviewVersion", exception.currentVersion());
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(detail);
    }

    @ExceptionHandler(RepairNotFoundException.class)
    ResponseEntity<ProblemDetail> missing(RepairNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(base(
                HttpStatus.NOT_FOUND,
                "REPAIR_RECORD_NOT_FOUND",
                exception.getMessage()));
    }

    private static ProblemDetail base(HttpStatus status, String code, String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle(status.getReasonPhrase());
        detail.setType(URI.create("urn:problem:" + code.toLowerCase()));
        detail.setProperty("code", code);
        return detail;
    }
}
