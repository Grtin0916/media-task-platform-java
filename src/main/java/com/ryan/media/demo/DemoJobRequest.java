package com.ryan.media.demo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
public record DemoJobRequest(
        @NotBlank String caseId, @NotBlank String mode,
        @Min(1) @Max(600) int timeoutSeconds, boolean resume) {}
