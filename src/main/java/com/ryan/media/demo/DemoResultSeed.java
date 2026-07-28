package com.ryan.media.demo;
public record DemoResultSeed(
        String caseId, String sourceCaseId, String repairDecision,
        DemoPublishDecision publishDecision, DemoArtifactRef selectedArtifact,
        DemoArtifactRef repairArtifact, String repairAction, boolean editApplied,
        boolean liveGroupAvailable, boolean humanPreferenceProven, boolean finalSelected) {}
