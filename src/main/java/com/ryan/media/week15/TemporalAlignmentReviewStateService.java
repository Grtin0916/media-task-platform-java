package com.ryan.media.week15;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemporalAlignmentReviewStateService {
    private final TemporalAlignmentReviewStateRegistry registry;

    public TemporalAlignmentReviewStateService(TemporalAlignmentReviewStateRegistry registry) {
        this.registry = registry;
    }

    public Map<String, Object> getReviewState() {
        return registry.loadReviewState();
    }
}
