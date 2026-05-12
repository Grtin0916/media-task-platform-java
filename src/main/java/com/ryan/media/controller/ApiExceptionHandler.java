package com.ryan.media.controller;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problem.setType(URI.create("https://media-task-platform-java/problems/media-task-not-found"));
        problem.setTitle("Media task not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "MEDIA_TASK_NOT_FOUND");

        return problem;
    }
}
