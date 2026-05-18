package com.ryan.media.service;

import com.ryan.media.messaging.Producer;
import com.ryan.media.model.CreateMediaTaskRequest;
import com.ryan.media.model.MediaTaskListResponse;
import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.repository.MediaTaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MediaTaskService {

    private final MediaTaskRepository mediaTaskRepository;
    private final Producer producer;

    public MediaTaskService(MediaTaskRepository mediaTaskRepository, Producer producer) {
        this.mediaTaskRepository = mediaTaskRepository;
        this.producer = producer;
    }

    public MediaTaskResponse create(CreateMediaTaskRequest request) {
        MediaTaskResponse task = new MediaTaskResponse(
                UUID.randomUUID().toString(),
                request.title(),
                request.mediaType(),
                "CREATED",
                Instant.now()
        );
        mediaTaskRepository.save(task);

        producer.publishMediaTaskCreated(
                task.id(),
                "week07-user",
                task.status(),
                "trace-" + task.id()
        );

        return task;
    }

    public List<MediaTaskResponse> list() {
        return mediaTaskRepository.findAll();
    }

    public MediaTaskListResponse list(int page, int size, String status, String sort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (sort == null || sort.isBlank()) {
            sort = "created_at_desc";
        }
        if (!sort.equals("created_at_desc") && !sort.equals("created_at_asc")) {
            throw new IllegalArgumentException("unsupported sort: " + sort);
        }
        if (status != null && status.isBlank()) {
            status = null;
        }

        int offset = page * size;
        var content = mediaTaskRepository.findPage(status, size, offset, sort);
        long totalElements = mediaTaskRepository.count(status);

        return new MediaTaskListResponse(content, page, size, totalElements, status, sort);
    }

    public MediaTaskResponse getById(String id) {
        return mediaTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("media task not found: " + id));
    }

    public void deleteById(String id) {
        int rows = mediaTaskRepository.deleteById(id);
        if (rows == 0) {
            throw new IllegalArgumentException("media task not found: " + id);
        }
    }
}
