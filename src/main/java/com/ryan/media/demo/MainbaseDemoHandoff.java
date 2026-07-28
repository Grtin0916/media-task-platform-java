package com.ryan.media.demo;
import java.util.List;
public record MainbaseDemoHandoff(
        String schemaVersion, String sourceCommit, String handoffDigest,
        List<DemoResultSeed> records, int provisionalCount, int blockedCount, int finalSelectedCount) {}
