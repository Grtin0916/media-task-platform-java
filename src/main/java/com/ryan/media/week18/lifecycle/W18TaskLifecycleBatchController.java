package com.ryan.media.week18.lifecycle;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/week18/lifecycle")
public class W18TaskLifecycleBatchController {

    private final W18TaskLifecycleBatchOrchestrator orchestrator;

    public W18TaskLifecycleBatchController(
            W18TaskLifecycleBatchOrchestrator orchestrator
    ) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/bootstrap")
    public ObjectNode bootstrapContract() {
        return orchestrator.bootstrap();
    }
}