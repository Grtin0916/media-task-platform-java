package com.ryan.media.controller;

import com.ryan.media.week15.TemporalAlignmentReviewStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class Week15TemporalAlignmentReviewStateController {
    private final TemporalAlignmentReviewStateService service;

    public Week15TemporalAlignmentReviewStateController(TemporalAlignmentReviewStateService service) {
        this.service = service;
    }

    @GetMapping("/api/week15/temporal-alignment-review-state")
    public Map<String, Object> getReviewState() {
        return service.getReviewState();
    }
}
