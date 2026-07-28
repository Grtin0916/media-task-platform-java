package com.ryan.media.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalRunnerProcessTest {
    @TempDir Path temp;
    private final LocalRunnerProcess runner = new LocalRunnerProcess(new DemoProcessTreeTerminator());

    @Test
    void preservesNonZeroExitCode() throws Exception {
        var running = runner.start(
                List.of("python3", Path.of("artifacts/fixtures/demo-job/fail_runner.py").toAbsolutePath().toString()),
                Path.of("."), temp.resolve("failed"));
        var outcome = runner.await(running, Duration.ofSeconds(5));
        assertFalse(outcome.timedOut());
        assertEquals(1, outcome.exitCode());
        assertTrue(Files.readString(running.stderr()).contains("fixture failure"));
    }

    @Test
    void timeoutTerminatesTheSpawnedProcessTree() throws Exception {
        var running = runner.start(
                List.of("python3", Path.of("artifacts/fixtures/demo-job/spawn_child_runner.py").toAbsolutePath().toString()),
                Path.of("."), temp.resolve("tree"));
        Thread.sleep(250);
        var outcome = runner.await(running, Duration.ofMillis(100));
        assertTrue(outcome.timedOut());
        assertTrue(outcome.termination().discovered() >= 1);
        assertEquals(0, outcome.termination().stillAlive());
    }
}
