package com.ryan.media.service;

import com.ryan.media.model.CreateMediaTaskRequest;
import com.ryan.media.model.MediaTaskResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MediaTaskService {

    private final ConcurrentHashMap<String, MediaTaskResponse> store = new ConcurrentHashMap<>();

    public MediaTaskResponse create(CreateMediaTaskRequest request) {
        String id = UUID.randomUUID().toString();
        MediaTaskResponse task = new MediaTaskResponse(
                id,
                request.title(),
                request.mediaType(),
                "CREATED",
                Instant.now()
        );
        store.put(id, task);
        return task;
    }

    public List<MediaTaskResponse> list() {
        return store.values()
                .stream()
                .sorted(Comparator.comparing(MediaTaskResponse::createdAt).reversed())
                .toList();
    }

    public MediaTaskResponse getById(String id) {
        MediaTaskResponse task = store.get(id);
        if (task == null) {
            throw new IllegalArgumentException("media task not found: " + id);
        }
        return task;
    }

    public void deleteById(String id) {
        MediaTaskResponse removed = store.remove(id);
        if (removed == null) {
            throw new IllegalArgumentException("media task not found: " + id);
        }
    }
}
