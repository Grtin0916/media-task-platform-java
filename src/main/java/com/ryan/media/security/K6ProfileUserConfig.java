package com.ryan.media.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@Profile("k6")
public class K6ProfileUserConfig {

    @Bean
    public UserDetailsService k6UserDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("k6-user")
                        .password("$2a$10$16ruzlsKheWauCL8ICUGfOJijWP4gHnlKFquwqSbrnpZI.ctcv5Qa")
                        .roles("USER")
                        .build()
        );
    }
}
