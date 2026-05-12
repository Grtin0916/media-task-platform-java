package com.ryan.media;

import com.ryan.media.auth.AuthController;
import com.ryan.media.controller.MediaTaskController;
import com.ryan.media.messaging.Consumer;
import com.ryan.media.security.SecurityConfig;
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
    void listMediaTasksShouldReturnJsonArrayForAuthenticatedUser() throws Exception {
        when(mediaTaskService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/media-tasks").with(user("week10-contract-user").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
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
}
