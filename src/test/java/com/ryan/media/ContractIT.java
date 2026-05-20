package com.ryan.media;

import java.time.Instant;
import com.ryan.media.auth.AuthController;
import com.ryan.media.controller.MediaTaskController;
import com.ryan.media.messaging.Consumer;
import com.ryan.media.security.SecurityConfig;
import com.ryan.media.model.MediaTaskListResponse;
import com.ryan.media.model.MediaTaskResponse;
import com.ryan.media.service.MediaTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, MediaTaskController.class})
@Import(SecurityConfig.class)
class ContractIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaTaskService mediaTaskService;

    @MockBean
    private Consumer consumer;

    @Test
    void authLoginShouldKeepWeek10SkeletonContract() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "week10-user",
                      "password": "week10-password"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("week06-skeleton"))
            .andExpect(jsonPath("$.issued").value(false))
            .andExpect(jsonPath("$.note").isString());
    }

    @Test
    void authMeShouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeShouldReturnCurrentPrincipalForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/auth/me").with(user("week10-contract-user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.name").value("week10-contract-user"))
            .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"));
    }

    @Test
    void listMediaTasksShouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/media-tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listMediaTasksShouldReturnPagedResponseForAuthenticatedUser() throws Exception {
        when(mediaTaskService.list(0, 5, "CREATED", "created_at_desc"))
                .thenReturn(new MediaTaskListResponse(List.of(), 0, 5, 0L, "CREATED", "created_at_desc"));

        mockMvc.perform(get("/api/media-tasks")
                .with(user("week10-contract-user").roles("USER"))
                .param("page", "0")
                .param("size", "5")
                .param("status", "CREATED")
                .param("sort", "created_at_desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.sort").value("created_at_desc"));
    }

    @Test
    void queryBadSortShouldReturnProblemDetail() throws Exception {
        when(mediaTaskService.list(0, 20, null, "bad_sort"))
                .thenThrow(new IllegalArgumentException("unsupported sort: bad_sort"));

        mockMvc.perform(get("/api/media-tasks")
                .with(user("contract-user"))
                .param("sort", "bad_sort"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MEDIA_TASK_INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("unsupported sort: bad_sort"));
    }

    @Test
    void createMediaTaskShouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/media-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "week10-contract-task",
                      "mediaType": "audio"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createMediaTaskShouldAcceptAuthenticatedUserAtContractBoundary() throws Exception {
        mockMvc.perform(post("/api/media-tasks")
                .with(user("week10-contract-user").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "week10-contract-task",
                      "mediaType": "audio"
                    }
                    """))
            .andExpect(status().isCreated());
    }

    @Test
    void getMediaTaskByIdShouldReturnProblemDetailWhenMissing() throws Exception {
        when(mediaTaskService.getById("missing-task"))
            .thenThrow(new IllegalArgumentException("media task not found: missing-task"));

        mockMvc.perform(get("/api/media-tasks/missing-task")
                .with(user("week10-contract-user").roles("USER")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://media-task-platform-java/problems/media-task-not-found"))
            .andExpect(jsonPath("$.title").value("Media task not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("media task not found: missing-task"))
            .andExpect(jsonPath("$.instance").value("/api/media-tasks/missing-task"))
            .andExpect(jsonPath("$.code").value("MEDIA_TASK_NOT_FOUND"));
    }

    @Test
    void createMediaTaskShouldAcceptIdempotencyKeyHeaderAsContractBoundary() throws Exception {
        mockMvc.perform(post("/api/media-tasks")
                .with(user("week10-contract-user").roles("USER"))
                .header("Idempotency-Key", "week10-idempotency-key-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "week10-idempotency-contract-task",
                      "mediaType": "audio"
                    }
                    """))
            .andExpect(status().isCreated());
    }

    @Test
    void queryNegativePageShouldReturnProblemDetail() throws Exception {
        when(mediaTaskService.list(-1, 20, null, "created_at_desc"))
                .thenThrow(new IllegalArgumentException("page must be >= 0"));

        mockMvc.perform(get("/api/media-tasks")
                .with(user("contract-user"))
                .param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MEDIA_TASK_INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("page must be >= 0"));
    }

    @Test
    void queryZeroSizeShouldReturnProblemDetail() throws Exception {
        when(mediaTaskService.list(0, 0, null, "created_at_desc"))
                .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));

        mockMvc.perform(get("/api/media-tasks")
                .with(user("contract-user"))
                .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MEDIA_TASK_INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }

    @Test
    void queryOversizedPageShouldReturnProblemDetail() throws Exception {
        when(mediaTaskService.list(0, 101, null, "created_at_desc"))
                .thenThrow(new IllegalArgumentException("size must be between 1 and 100"));

        mockMvc.perform(get("/api/media-tasks")
                .with(user("contract-user"))
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("MEDIA_TASK_INVALID_REQUEST"))
            .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }


    @Test
    void listMediaTasksShouldExposeEvalArtifactLinksForWeek11SeedTask() throws Exception {
        when(mediaTaskService.list(0, 5, "CREATED", "created_at_desc"))
                .thenReturn(new MediaTaskListResponse(
                        List.of(new MediaTaskResponse(
                                "week11-k6-seed-created-001",
                                "Week11 seeded media task",
                                "video",
                                "CREATED",
                                Instant.parse("2026-05-20T00:00:00Z"),
                                "mainbase://artifacts/manifests/week11_crossrepo_task_bridge.json",
                                "mainbase://artifacts/evals/week11_eval_quality_gate_v0.json",
                                "PASS"
                        )),
                        0,
                        5,
                        1L,
                        "CREATED",
                        "created_at_desc"
                ));

        mockMvc.perform(get("/api/media-tasks")
                        .with(user("contract-user").roles("USER"))
                        .param("page", "0")
                        .param("size", "5")
                        .param("status", "CREATED")
                        .param("sort", "created_at_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("week11-k6-seed-created-001"))
                .andExpect(jsonPath("$.content[0].artifactUri").value("mainbase://artifacts/manifests/week11_crossrepo_task_bridge.json"))
                .andExpect(jsonPath("$.content[0].evalSummaryUri").value("mainbase://artifacts/evals/week11_eval_quality_gate_v0.json"))
                .andExpect(jsonPath("$.content[0].qualityGateStatus").value("PASS"));
    }

}
