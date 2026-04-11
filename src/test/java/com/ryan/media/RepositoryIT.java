package com.ryan.media;

import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.repository.MediaTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MediaTaskApplication.class)
@Testcontainers
class RepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MediaTaskRepository mediaTaskRepository;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void cleanTables() {
        jdbcClient.sql("delete from media_tag").update();
        jdbcClient.sql("delete from media_asset").update();
        jdbcClient.sql("delete from media_task").update();
    }

    @Test
    void save_and_findById_should_persist_task() {
        MediaTaskResponse task = new MediaTaskResponse(
                "task-001",
                "seed mapping task",
                "video",
                "CREATED",
                Instant.parse("2026-04-11T02:00:00Z")
        );

        mediaTaskRepository.save(task);

        Optional<MediaTaskResponse> found = mediaTaskRepository.findById("task-001");
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo("task-001");
        assertThat(found.get().title()).isEqualTo("seed mapping task");
        assertThat(found.get().mediaType()).isEqualTo("video");
        assertThat(found.get().status()).isEqualTo("CREATED");
    }

    @Test
    void findAll_should_return_tasks_in_desc_createdAt_order() {
        mediaTaskRepository.save(new MediaTaskResponse(
                "task-001",
                "older",
                "video",
                "CREATED",
                Instant.parse("2026-04-11T01:00:00Z")
        ));
        mediaTaskRepository.save(new MediaTaskResponse(
                "task-002",
                "newer",
                "audio",
                "CREATED",
                Instant.parse("2026-04-11T03:00:00Z")
        ));

        List<MediaTaskResponse> tasks = mediaTaskRepository.findAll();

        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).id()).isEqualTo("task-002");
        assertThat(tasks.get(1).id()).isEqualTo("task-001");
    }

    @Test
    void deleteById_should_remove_existing_task() {
        mediaTaskRepository.save(new MediaTaskResponse(
                "task-003",
                "to-delete",
                "video",
                "CREATED",
                Instant.parse("2026-04-11T04:00:00Z")
        ));

        int deleted = mediaTaskRepository.deleteById("task-003");

        assertThat(deleted).isEqualTo(1);
        assertThat(mediaTaskRepository.findById("task-003")).isEmpty();
    }

    @Test
    void deleteById_should_return_zero_for_missing_task() {
        int deleted = mediaTaskRepository.deleteById("missing-id");
        assertThat(deleted).isEqualTo(0);
    }
}
