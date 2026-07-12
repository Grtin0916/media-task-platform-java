package com.ryan.media.week18.lifecycle;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class W18TaskLifecycleConfiguration {

    @Bean
    public W18TaskLifecycleService w18TaskLifecycleService(
            ObjectMapper objectMapper,
            @Value(
                    "${w18.lifecycle.contract-path:"
                            + "artifacts/week18/"
                            + "w18_selector_repair_handoff_20260708.json}"
            )
            String contractPath
    ) throws IOException {
        return new W18TaskLifecycleService(
                objectMapper,
                Path.of(contractPath)
        );
    }
}