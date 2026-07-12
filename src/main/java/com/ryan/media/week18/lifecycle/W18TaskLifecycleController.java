package com.ryan.media.week18.lifecycle;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/week18/lifecycle")
public class W18TaskLifecycleController {

    private final W18TaskLifecycleService service;

    public W18TaskLifecycleController(
            W18TaskLifecycleService service
    ) {
        this.service = service;
    }

    @PostMapping("/tasks")
    public ResponseEntity<W18TaskLifecycleService.Task> createTask(
            @RequestBody CreateTaskRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Request body must not be null"
            );
        }

        W18TaskLifecycleService.Task task = service.create(
                request.caseId(),
                request.artifactKind(),
                request.artifactKey()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(task);
    }

    @GetMapping("/tasks/{taskId}")
    public W18TaskLifecycleService.Task getTask(
            @PathVariable String taskId
    ) {
        return service.get(taskId);
    }

    @PostMapping("/tasks/{taskId}/queue")
    public W18TaskLifecycleService.Task queueTask(
            @PathVariable String taskId
    ) {
        return service.queue(taskId);
    }

    @PostMapping("/tasks/{taskId}/start")
    public W18TaskLifecycleService.Task startTask(
            @PathVariable String taskId
    ) {
        return service.start(taskId);
    }

    @PostMapping("/tasks/{taskId}/decide")
    public W18TaskLifecycleService.Task decideTask(
            @PathVariable String taskId
    ) {
        return service.decide(taskId);
    }

    @PostMapping("/tasks/{taskId}/repair")
    public W18TaskLifecycleService.Task applyRepair(
            @PathVariable String taskId
    ) {
        return service.applyRepair(taskId);
    }

    @PostMapping("/tasks/{taskId}/bind-result")
    public W18TaskLifecycleService.Task bindResult(
            @PathVariable String taskId
    ) {
        return service.bindResult(taskId);
    }

    @GetMapping("/tasks/{taskId}/result-card")
    public ObjectNode getResultCard(
            @PathVariable String taskId
    ) {
        return service.resultCard(taskId);
    }

    @GetMapping("/report")
    public ObjectNode getLifecycleReport() {
        return service.lifecycleReport();
    }

    public record CreateTaskRequest(
            String caseId,
            W18TaskLifecycleService.ArtifactKind artifactKind,
            String artifactKey
    ) {
    }
}