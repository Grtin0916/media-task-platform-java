package com.ryan.media.demo;
import java.util.List;
import org.springframework.stereotype.Component;
@Component
public class DemoRunnerCommandFactory {
    private final DemoRunnerProperties properties;
    public DemoRunnerCommandFactory(DemoRunnerProperties properties){this.properties=properties;}
    public List<String> command(DemoResultSeed seed) {
        if (!seed.sourceCaseId().matches("[A-Za-z0-9_-]+")) throw new DemoJobException("INVALID_CASE_ID",seed.sourceCaseId());
        return List.of(properties.python(),properties.script(),"run","--config",properties.config(),
                "--case-id",seed.sourceCaseId(),"--mode","replay");
    }
}
