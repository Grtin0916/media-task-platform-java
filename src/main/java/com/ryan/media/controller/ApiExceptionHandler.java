package com.ryan.media.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String detail = ex.getMessage() == null ? "Invalid request" : ex.getMessage();
        boolean mediaTaskNotFound = detail.startsWith("media task not found:");

        HttpStatus status = mediaTaskNotFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        if (mediaTaskNotFound) {
            problem.setType(URI.create("https://media-task-platform-java/problems/media-task-not-found"));
            problem.setTitle("Media task not found");
            problem.setProperty("code", "MEDIA_TASK_NOT_FOUND");
        } else {
            problem.setType(URI.create("https://media-task-platform-java/problems/invalid-request"));
            problem.setTitle("Invalid request");
            problem.setProperty("code", "MEDIA_TASK_INVALID_REQUEST");
        }

        if (request != null) {
            problem.setInstance(URI.create(request.getRequestURI()));
        }

        return problem;
    }
}
