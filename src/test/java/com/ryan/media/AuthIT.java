package com.ryan.media;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ryan.media.auth.AuthController;
import com.ryan.media.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginEndpointIsPublic() throws Exception {
        mvc.perform(post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"username":"week06","password":"demo"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("week06-skeleton"))
            .andExpect(jsonPath("$.issued").value(false));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsPrincipalForAuthenticatedUser() throws Exception {
        mvc.perform(get("/auth/me").with(user("week06").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.name").value("week06"));
    }
}
