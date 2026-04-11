package com.ryan.media.service;

import com.ryan.media.model.CreateMediaTaskRequest;
import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.repository.MediaTaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MediaTaskService {

    private final MediaTaskRepository mediaTaskRepository;

    public MediaTaskService(MediaTaskRepository mediaTaskRepository) {
        this.mediaTaskRepository = mediaTaskRepository;
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
        return task;
    }

    public List<MediaTaskResponse> list() {
        return mediaTaskRepository.findAll();
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
