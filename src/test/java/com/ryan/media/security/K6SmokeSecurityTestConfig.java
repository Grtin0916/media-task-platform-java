package com.ryan.media.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@Profile("k6-smoke")
public class K6SmokeSecurityTestConfig {

    @Bean
    UserDetailsService k6SmokeUserDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("k6-user")
                        .password(passwordEncoder.encode("k6-password"))
                        .roles("USER")
                        .build()
        );
    }
}
