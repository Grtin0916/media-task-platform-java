package com.ryan.media.demo;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(basePackageClasses=DemoJobController.class)
public class DemoJobExceptionHandler {
    @ExceptionHandler(DemoJobException.class)
    ResponseEntity<ProblemDetail> handle(DemoJobException e){
        HttpStatus status=switch(e.code()){
            case "JOB_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "IDEMPOTENCY_CONFLICT" -> HttpStatus.CONFLICT;
            case "INVALID_CASE_ID","IDEMPOTENCY_KEY_REQUIRED" -> HttpStatus.BAD_REQUEST;
            case "RETRY_NOT_ALLOWED","CANCEL_NOT_ALLOWED","INVALID_JOB_TRANSITION" -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        ProblemDetail problem=ProblemDetail.forStatusAndDetail(status,e.getMessage());problem.setProperty("code",e.code());
        return ResponseEntity.status(status).body(problem);
    }
}
