package com.ryan.media.demo;
public record DemoJobResultCard(
        String jobId, DemoJobStatus executionStatus, DemoPublishDecision publishDecision,
        boolean finalSelected, boolean proxyEvidenceOnly, boolean humanReviewCompleted,
        boolean liveGroupAvailable, String sourceCommit, String resultDigest, String integrityStatus) {}
