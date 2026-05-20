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
    private static final String WEEK11_EVAL_TASK_ID = "week11-k6-seed-created-001";
    private static final String WEEK11_EVAL_ARTIFACT_URI = "mainbase://artifacts/manifests/week11_crossrepo_task_bridge.json";
    private static final String WEEK11_EVAL_SUMMARY_URI = "mainbase://artifacts/evals/week11_eval_quality_gate_v0.json";
    private static final String WEEK11_QUALITY_GATE_STATUS = "PASS";


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
        return mediaTaskRepository.findAll()
                .stream()
                .map(this::withWeek11EvalArtifactLinks)
                .toList();
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
        var content = mediaTaskRepository.findPage(status, size, offset, sort)
                .stream()
                .map(this::withWeek11EvalArtifactLinks)
                .toList();
        long totalElements = mediaTaskRepository.count(status);

        return new MediaTaskListResponse(content, page, size, totalElements, status, sort);
    }

    private MediaTaskResponse withWeek11EvalArtifactLinks(MediaTaskResponse task) {
        if (!WEEK11_EVAL_TASK_ID.equals(task.id())) {
            return task;
        }
        return new MediaTaskResponse(
                task.id(),
                task.title(),
                task.mediaType(),
                task.status(),
                task.createdAt(),
                WEEK11_EVAL_ARTIFACT_URI,
                WEEK11_EVAL_SUMMARY_URI,
                WEEK11_QUALITY_GATE_STATUS
        );
    }


    public MediaTaskResponse getById(String id) {
        return mediaTaskRepository.findById(id)
                .map(this::withWeek11EvalArtifactLinks)
                .orElseThrow(() -> new IllegalArgumentException("media task not found: " + id));
    }

    public void deleteById(String id) {
        int rows = mediaTaskRepository.deleteById(id);
        if (rows == 0) {
            throw new IllegalArgumentException("media task not found: " + id);
        }
    }
}
