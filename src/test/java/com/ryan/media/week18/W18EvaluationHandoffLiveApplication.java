package com.ryan.media.week18;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = "com.ryan.media.week18",
    exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class,
        SecurityAutoConfiguration.class
    }
)
public class W18EvaluationHandoffLiveApplication {
}
