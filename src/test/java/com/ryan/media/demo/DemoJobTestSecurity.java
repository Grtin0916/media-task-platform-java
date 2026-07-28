package com.ryan.media.demo;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@TestConfiguration
public class DemoJobTestSecurity {
    @Bean SecurityFilterChain demoSecurity(HttpSecurity http)throws Exception{
        return http.csrf(x->x.disable()).authorizeHttpRequests(x->x.anyRequest().permitAll()).build();
    }
}
