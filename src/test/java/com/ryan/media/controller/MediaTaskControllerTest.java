package com.ryan.media.controller;

import com.ryan.media.service.MediaTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(MediaTaskController.class)
class MediaTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private MediaTaskService mediaTaskService;

    @Test
    void list_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/media-tasks"))
                .andExpect(status().isOk());
    }
}
