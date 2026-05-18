package com.ryan.media;

import com.ryan.media.messaging.Consumer;
import com.ryan.media.messaging.Producer;
import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.repository.MediaTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:week11_query_http_it;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class MediaTaskQueryHttpIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private MediaTaskRepository repository;

    @MockBean
    private Producer producer;

    @MockBean
    private Consumer consumer;

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

        when(consumer.streamKey()).thenReturn("week11-test-stream");
        when(consumer.consumerGroup()).thenReturn("week11-test-group");
        when(consumer.consumerName()).thenReturn("week11-test-consumer");
    }

    @Test
    void getMediaTasksShouldUseRealServiceRepositoryStatusFilterAndDescSort() throws Exception {
        String token = Long.toString(System.nanoTime(), 36).substring(0, 6);
        String statusCreated = "W11C" + token;
        String statusFailed = "W11F" + token;

        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-older",
                "Week11 HTTP Older",
                "audio",
                statusCreated,
                Instant.parse("2099-03-01T00:00:01Z")
        ));
        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-middle",
                "Week11 HTTP Middle",
                "video",
                statusCreated,
                Instant.parse("2099-03-01T00:00:02Z")
        ));
        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-newer",
                "Week11 HTTP Newer",
                "video",
                statusCreated,
                Instant.parse("2099-03-01T00:00:03Z")
        ));
        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-other-status",
                "Week11 HTTP Other Status",
                "video",
                statusFailed,
                Instant.parse("2099-03-01T00:00:04Z")
        ));

        mockMvc.perform(get("/api/media-tasks")
                        .with(user("week11-http-user").roles("USER"))
                        .param("page", "0")
                        .param("size", "2")
                        .param("status", statusCreated)
                        .param("sort", "created_at_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        "week11-http-" + token + "-newer",
                        "week11-http-" + token + "-middle"
                )))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.status").value(statusCreated))
                .andExpect(jsonPath("$.sort").value("created_at_desc"));
    }

    @Test
    void getMediaTasksShouldUseRealServiceRepositoryAscSortAndOffset() throws Exception {
        String token = Long.toString(System.nanoTime(), 36).substring(0, 6);
        String statusCreated = "W11C" + token;

        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-oldest",
                "Week11 HTTP Oldest",
                "audio",
                statusCreated,
                Instant.parse("2099-04-01T00:00:01Z")
        ));
        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-middle",
                "Week11 HTTP Middle",
                "video",
                statusCreated,
                Instant.parse("2099-04-01T00:00:02Z")
        ));
        repository.save(new MediaTaskResponse(
                "week11-http-" + token + "-newest",
                "Week11 HTTP Newest",
                "video",
                statusCreated,
                Instant.parse("2099-04-01T00:00:03Z")
        ));

        mockMvc.perform(get("/api/media-tasks")
                        .with(user("week11-http-user").roles("USER"))
                        .param("page", "1")
                        .param("size", "1")
                        .param("status", statusCreated)
                        .param("sort", "created_at_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        "week11-http-" + token + "-middle"
                )))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.status").value(statusCreated))
                .andExpect(jsonPath("$.sort").value("created_at_asc"));
    }

    @Test
    void getMediaTasksShouldRejectInvalidQueryBoundary() throws Exception {
        mockMvc.perform(get("/api/media-tasks")
                        .with(user("week11-http-user").roles("USER"))
                        .param("page", "-1")
                        .param("size", "5")
                        .param("sort", "created_at_desc"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/media-tasks")
                        .with(user("week11-http-user").roles("USER"))
                        .param("page", "0")
                        .param("size", "101")
                        .param("sort", "created_at_desc"))
                .andExpect(status().is4xxClientError());
    }
}
