package com.ryan.media;

import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.repository.MediaTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = "spring.flyway.enabled=false")
@Import(MediaTaskRepository.class)
class MediaTaskQueryIT {

    @Autowired
    private MediaTaskRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void setUpSchema() {
        jdbcClient.sql("drop table if exists media_task").update();

        jdbcClient.sql("""
                create table media_task (
                    id varchar(100) primary key,
                    title varchar(255) not null,
                    media_type varchar(50) not null,
                    status varchar(50) not null,
                    created_at timestamp not null,
                    updated_at timestamp not null
                )
                """).update();

        jdbcClient.sql("""
                create index idx_media_task_created_at_desc
                on media_task (created_at desc)
                """).update();

        jdbcClient.sql("""
                create index idx_media_task_status_created_at_desc
                on media_task (status, created_at desc)
                """).update();
    }

    @Test
    void findPageShouldFilterByStatusAndSortByCreatedAtDesc() {
        String token = Long.toString(System.nanoTime(), 36).substring(0, 6);
        String statusCreated = "W11C" + token;
        String statusFailed = "W11F" + token;

        MediaTaskResponse older = new MediaTaskResponse(
                "week11-query-" + token + "-older",
                "Week11 Query Older",
                "audio",
                statusCreated,
                Instant.parse("2099-01-01T00:00:01Z")
        );
        MediaTaskResponse middle = new MediaTaskResponse(
                "week11-query-" + token + "-middle",
                "Week11 Query Middle",
                "video",
                statusCreated,
                Instant.parse("2099-01-01T00:00:02Z")
        );
        MediaTaskResponse newer = new MediaTaskResponse(
                "week11-query-" + token + "-newer",
                "Week11 Query Newer",
                "video",
                statusCreated,
                Instant.parse("2099-01-01T00:00:03Z")
        );
        MediaTaskResponse otherStatus = new MediaTaskResponse(
                "week11-query-" + token + "-other-status",
                "Week11 Query Other Status",
                "video",
                statusFailed,
                Instant.parse("2099-01-01T00:00:04Z")
        );

        repository.save(older);
        repository.save(middle);
        repository.save(newer);
        repository.save(otherStatus);

        List<MediaTaskResponse> firstPage = repository.findPage(statusCreated, 2, 0, "created_at_desc");

        assertThat(firstPage)
                .extracting(MediaTaskResponse::id)
                .containsExactly(newer.id(), middle.id());

        assertThat(repository.count(statusCreated)).isEqualTo(3L);
        assertThat(repository.count(statusFailed)).isEqualTo(1L);
    }

    @Test
    void findPageShouldSupportAscSortAndOffset() {
        String token = Long.toString(System.nanoTime(), 36).substring(0, 6);
        String statusCreated = "W11C" + token;

        MediaTaskResponse oldest = new MediaTaskResponse(
                "week11-query-" + token + "-oldest",
                "Week11 Query Oldest",
                "audio",
                statusCreated,
                Instant.parse("2099-02-01T00:00:01Z")
        );
        MediaTaskResponse middle = new MediaTaskResponse(
                "week11-query-" + token + "-middle",
                "Week11 Query Middle",
                "video",
                statusCreated,
                Instant.parse("2099-02-01T00:00:02Z")
        );
        MediaTaskResponse newest = new MediaTaskResponse(
                "week11-query-" + token + "-newest",
                "Week11 Query Newest",
                "video",
                statusCreated,
                Instant.parse("2099-02-01T00:00:03Z")
        );

        repository.save(oldest);
        repository.save(middle);
        repository.save(newest);

        List<MediaTaskResponse> offsetPage = repository.findPage(statusCreated, 2, 1, "created_at_asc");

        assertThat(offsetPage)
                .extracting(MediaTaskResponse::id)
                .containsExactly(middle.id(), newest.id());
    }
}
