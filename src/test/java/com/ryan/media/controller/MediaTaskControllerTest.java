package com.ryan.media.controller;

import com.ryan.media.service.MediaTaskService;
import com.ryan.media.messaging.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaTaskControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MediaTaskService mediaTaskService = mock(MediaTaskService.class);
        Consumer consumer = mock(Consumer.class);
        MediaTaskController controller = new MediaTaskController(mediaTaskService, consumer);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void list_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/media-tasks"))
            .andExpect(status().isOk());
    }
}
