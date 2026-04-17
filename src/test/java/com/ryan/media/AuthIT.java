package com.ryan.media;

import com.ryan.media.auth.AuthController;
import com.ryan.media.controller.MediaTaskController;
import com.ryan.media.security.SecurityConfig;
import com.ryan.media.service.MediaTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, MediaTaskController.class})
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
class AuthIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaTaskService mediaTaskService;

    @Test
    void login_should_be_public() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "demo",
                      "password": "demo"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void authMe_should_return_401_when_unauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void mediaTasks_should_return_401_when_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/media-tasks"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo-user")
    void authMe_should_return_200_when_authenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isOk());
    }

    @Test
    void createMediaTask_should_return_401_when_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/media-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "demo-task"
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "demo-user")
    void createMediaTask_should_return_400_when_authenticated_but_payload_invalid() throws Exception {
        mockMvc.perform(post("/api/media-tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "demo-task"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }
}
