package com.ryan.media.ranking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = RankerController.class)
public class RankerExceptionHandler {
    @ExceptionHandler(RankerException.class)
    ResponseEntity<ProblemDetail> handle(RankerException exception) {
        HttpStatus status = switch (exception.code()) {
            case "RANKER_VERSION_NOT_FOUND", "RANKER_BUNDLE_MISSING",
                    "RANKER_ARTIFACT_MISSING" -> HttpStatus.NOT_FOUND;
            case "RANKER_VERSION_CONFLICT" -> HttpStatus.CONFLICT;
            case "RANKER_PATH_OUTSIDE_ALLOWED_ROOT" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setProperty("code", exception.code());
        if (exception.rankerVersion() != null) {
            problem.setProperty("rankerVersion", exception.rankerVersion());
        }
        if (exception.bundleDigest() != null) {
            problem.setProperty("bundleDigest", exception.bundleDigest());
        }
        if (exception.promotionStatus() != null) {
            problem.setProperty("promotionStatus", exception.promotionStatus());
        }
        return ResponseEntity.status(status).body(problem);
    }
}
