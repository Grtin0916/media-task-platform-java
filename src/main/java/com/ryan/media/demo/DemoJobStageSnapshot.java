package com.ryan.media.demo;
import java.util.Map;
public record DemoJobStageSnapshot(
        String stageId, String status, String executionMode, boolean reused,
        Map<String, String> inputDigests, Map<String, String> outputDigests,
        long durationMs, String failureReason) {}
