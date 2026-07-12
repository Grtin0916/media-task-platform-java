package com.ryan.media.week18.lifecycle;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = {
                W18TaskLifecycleController.class,
                W18TaskLifecycleBatchController.class
        }
)
public class W18TaskLifecycleExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        String message = safeMessage(exception);

        HttpStatus status;
        String code;

        if (message.startsWith("Task not found")) {
            status = HttpStatus.NOT_FOUND;
            code = "W18_TASK_NOT_FOUND";
        } else {
            status = HttpStatus.BAD_REQUEST;
            code = "W18_BAD_REQUEST";
        }

        return buildProblem(
                status,
                code,
                message,
                request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return buildProblem(
                HttpStatus.CONFLICT,
                "W18_INVALID_STATE_TRANSITION",
                safeMessage(exception),
                request
        );
    }

    private ResponseEntity<ProblemDetail> buildProblem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );

        problem.setTitle(status.getReasonPhrase());
        problem.setType(
                URI.create(
                        "urn:problem:"
                                + code.toLowerCase(Locale.ROOT)
                                .replace('_', '-')
                )
        );
        problem.setInstance(
                URI.create(request.getRequestURI())
        );
        problem.setProperty("code", code);
        problem.setProperty(
                "timestamp",
                Instant.now().toString()
        );

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }
}