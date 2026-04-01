package com.ryan.media.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryan.media.model.CreateMediaTaskRequest;
import com.ryan.media.service.MediaTaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MediaTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaTaskService mediaTaskService;

    @Test
    @DisplayName("POST /api/media-tasks should create a task")
    void createTask() throws Exception {
        CreateMediaTaskRequest request = new CreateMediaTaskRequest("demo-task", "audio");

        mockMvc.perform(post("/api/media-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.title").value("demo-task"))
                .andExpect(jsonPath("$.mediaType").value("audio"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("GET /api/media-tasks should return a list")
    void listTasks() throws Exception {
        mediaTaskService.create(new CreateMediaTaskRequest("task-1", "audio"));
        mediaTaskService.create(new CreateMediaTaskRequest("task-2", "video"));

        mockMvc.perform(get("/api/media-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/media-tasks/{id} should return task")
    void getTaskById() throws Exception {
        String id = mediaTaskService.create(new CreateMediaTaskRequest("task-get", "audio")).id();

        mockMvc.perform(get("/api/media-tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("task-get"));
    }

    @Test
    @DisplayName("DELETE /api/media-tasks/{id} should delete task")
    void deleteTaskById() throws Exception {
        String id = mediaTaskService.create(new CreateMediaTaskRequest("task-delete", "audio")).id();

        mockMvc.perform(delete("/api/media-tasks/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/media-tasks with blank title should fail")
    void createTaskValidationFail() throws Exception {
        String body = """
                {
                  "title": "",
                  "mediaType": "audio"
                }
                """;

        mockMvc.perform(post("/api/media-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
