package com.ryan.media.controller;

import com.ryan.media.model.CreateMediaTaskRequest;
import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.service.MediaTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media-tasks")
public class MediaTaskController {

    private final MediaTaskService mediaTaskService;

    public MediaTaskController(MediaTaskService mediaTaskService) {
        this.mediaTaskService = mediaTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MediaTaskResponse create(@Valid @RequestBody CreateMediaTaskRequest request) {
        return mediaTaskService.create(request);
    }

    @GetMapping
    public List<MediaTaskResponse> list() {
        return mediaTaskService.list();
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        return Map.of(
                "code", "MEDIA_TASK_NOT_FOUND",
                "message", ex.getMessage()
        );
    }
}
