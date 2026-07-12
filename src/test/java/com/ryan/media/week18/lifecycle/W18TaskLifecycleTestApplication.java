package com.ryan.media.week18.lifecycle;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(
        excludeName = {
                "org.springframework.boot.autoconfigure.jdbc."
                        + "DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.flyway."
                        + "FlywayAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet."
                        + "SecurityAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet."
                        + "SecurityFilterAutoConfiguration",
                "org.springframework.boot.actuate.autoconfigure.security.servlet."
                        + "ManagementWebSecurityAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet."
                        + "UserDetailsServiceAutoConfiguration",
                "org.springframework.boot.autoconfigure.data.redis."
                        + "RedisAutoConfiguration"
        }
)
@Import({
        W18TaskLifecycleConfiguration.class,
        W18TaskLifecycleMetricsConfiguration.class,
        W18TaskLifecycleController.class,
        W18TaskLifecycleBatchOrchestrator.class,
        W18TaskLifecycleBatchController.class,
        W18TaskLifecycleExceptionHandler.class
})
public class W18TaskLifecycleTestApplication {
}