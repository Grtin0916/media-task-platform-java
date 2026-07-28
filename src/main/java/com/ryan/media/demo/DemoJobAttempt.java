package com.ryan.media.demo;
import java.time.Instant;
import java.util.List;
public record DemoJobAttempt(
        String attemptId, int attemptNumber, String status, String commandDigest,
        String workingDirectory, long pid, Instant startedAt, Instant endedAt, long durationMs,
        Integer exitCode, String stdoutPath, String stderrPath, String runManifestPath,
        String runManifestDigest, String resultDigest, String failureCode, String failureReason,
        int descendantsDiscovered, int descendantsTerminated, int descendantsStillAlive,
        List<DemoJobStageSnapshot> stages) {}
