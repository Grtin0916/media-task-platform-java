package com.ryan.media.demo;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class DemoRunnerProperties {
    private final String python,script,config;
    private final Path mainbaseRoot,javaRoot;
    private final int maxConcurrentJobs;
    private final Duration cancelGrace;
    public DemoRunnerProperties(
            @Value("${demo.runner.python:python3}") String python,
            @Value("${demo.runner.mainbase-root:../audio_engineering_repo_skeleton_v1}") String root,
            @Value("${demo.runner.java-root:.}") String javaRoot,
            @Value("${demo.runner.script:scripts/run_demo.py}") String script,
            @Value("${demo.runner.config:configs/demo/runner.yaml}") String config,
            @Value("${demo.runner.max-concurrent-jobs:2}") int max,
            @Value("${demo.runner.cancel-grace-period-ms:500}") long grace) {
        this.python=python;this.mainbaseRoot=Path.of(root).toAbsolutePath().normalize();
        this.javaRoot=Path.of(javaRoot).toAbsolutePath().normalize();this.script=script;this.config=config;
        this.maxConcurrentJobs=max;this.cancelGrace=Duration.ofMillis(grace);
    }
    public String python(){return python;} public Path mainbaseRoot(){return mainbaseRoot;}
    public Path javaRoot(){return javaRoot;} public String script(){return script;}
    public String config(){return config;} public int maxConcurrentJobs(){return maxConcurrentJobs;}
    public Duration cancelGrace(){return cancelGrace;}
}
