package com.ryan.media.controller;

import com.ryan.media.week15.TemporalAlignmentExplicitRiskContractService;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week15TemporalAlignmentExplicitRiskContractController {

    private final TemporalAlignmentExplicitRiskContractService service;

    public Week15TemporalAlignmentExplicitRiskContractController(
            TemporalAlignmentExplicitRiskContractService service) {
        this.service = service;
    }

    @GetMapping("/api/week15/temporal-alignment/explicit-risk-contract")
    public ResponseEntity<Map<String, Object>> explicitRiskContract() throws IOException {
        return ResponseEntity.ok(service.readContract());
    }
}
