package com.ryan.media;

import com.ryan.media.auth.AuthController;
import com.ryan.media.controller.MediaTaskController;
import com.ryan.media.security.SecurityConfig;
import com.ryan.media.service.MediaTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, MediaTaskController.class})
@Import(SecurityConfig.class)
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaTaskService mediaTaskService;

    @Test
    void authMeShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeShouldReturn200WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me").with(user("week06-user").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void listMediaTasksShouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/media-tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMediaTasksShouldReturn200WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/media-tasks").with(user("week06-user").roles("USER")))
                .andExpect(status().isOk());
    }
}
