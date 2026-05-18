package com.ryan.media.controller;

import com.ryan.media.messaging.Consumer;
import com.ryan.media.model.CreateMediaTaskRequest;
import com.ryan.media.model.MediaTaskListResponse;
import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.service.MediaTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/media-tasks")
public class MediaTaskController {

    private final MediaTaskService mediaTaskService;
    private final Consumer consumer;

    public MediaTaskController(MediaTaskService mediaTaskService, Consumer consumer) {
        this.mediaTaskService = mediaTaskService;
        this.consumer = consumer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaTaskResponse create(@Valid @RequestBody CreateMediaTaskRequest request) {
        return mediaTaskService.create(request);
    }

    @GetMapping
    public MediaTaskListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "created_at_desc") String sort
    ) {
        return mediaTaskService.list(page, size, status, sort);
    }

    @GetMapping("/{id}")
    public MediaTaskResponse getById(@PathVariable String id) {
        return mediaTaskService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable String id) {
        mediaTaskService.deleteById(id);
    }

    @PostMapping("/eventing/smoke-consume")
    public Map<String, Object> smokeConsume() {
        String result = consumer.consumeOneForWeek07Smoke();
        return Map.of(
                "code", "EVENTING_SMOKE_RESULT",
                "result", result,
                "streamKey", consumer.streamKey(),
                "consumerGroup", consumer.consumerGroup(),
                "consumerName", consumer.consumerName()
        );
    }
}
