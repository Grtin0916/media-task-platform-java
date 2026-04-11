package com.ryan.media;

import com.ryan.media.repository.MediaTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
        }
)
class SmokeTest {

    @MockBean
    private MediaTaskRepository mediaTaskRepository;

    @Test
    void contextLoads() {
    }
}
