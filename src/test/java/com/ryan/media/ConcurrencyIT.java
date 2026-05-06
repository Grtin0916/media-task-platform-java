package com.ryan.media;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyIT {

    private static final int TASKS = 160;
    private static final int PLATFORM_THREADS = 8;
    private static final long BLOCKING_SLEEP_MS = 40L;

    @Test
    void compare_platform_threads_and_virtual_threads_for_blocking_tasks() throws Exception {
        Result platform = runBlockingTasks(
                "platform-fixed-" + PLATFORM_THREADS,
                Executors.newFixedThreadPool(PLATFORM_THREADS)
        );

        Result virtual = runBlockingTasks(
                "virtual-thread-per-task",
                Executors.newVirtualThreadPerTaskExecutor()
        );

        writeEvidence(platform, virtual);

        assertEquals(TASKS, platform.completedTasks());
        assertEquals(TASKS, virtual.completedTasks());

        /*
         * This is intentionally a broad assertion.
         * The point is not to claim virtual threads are always faster.
         * The point is to verify their advantage in many concurrent blocking waits.
         */
        assertTrue(
                virtual.elapsedMillis() < platform.elapsedMillis(),
                "virtual threads should complete this blocking-wait workload faster than a small fixed platform-thread pool"
        );
    }

    private Result runBlockingTasks(String label, ExecutorService executor) throws Exception {
        long started = System.nanoTime();

        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < TASKS; i++) {
                final int taskId = i;
                tasks.add(() -> {
                    Thread.sleep(BLOCKING_SLEEP_MS);
                    return taskId;
                });
            }

            List<Future<Integer>> futures = executor.invokeAll(tasks);

            int completed = 0;
            for (Future<Integer> future : futures) {
                future.get();
                completed++;
            }

            long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
            return new Result(label, completed, elapsedMillis);
        } finally {
            executor.shutdownNow();
        }
    }

    private void writeEvidence(Result platform, Result virtual) throws IOException {
        Path out = Path.of("artifacts/logs/week09_concurrency_it_20260506.csv");
        Files.createDirectories(out.getParent());

        StringBuilder text = new StringBuilder();
        text.append("date,label,tasks,blocking_sleep_ms,elapsed_ms,throughput_tasks_per_sec,notes\n");
        appendRow(text, platform, "fixed platform thread pool baseline");
        appendRow(text, virtual, "virtual thread per task executor");

        Files.writeString(out, text.toString());

        Path md = Path.of("artifacts/logs/week09_concurrency_it_20260506.md");
        Files.writeString(md, """
                # Week09 ConcurrencyIT Evidence

                ## Scope

                This test compares a small fixed platform-thread pool with Java 21 virtual threads
                under a blocking-wait workload.

                ## Experiment setup

                - Date: %s
                - Tasks: %d
                - Blocking sleep per task: %d ms
                - Platform executor: fixed thread pool, size=%d
                - Virtual executor: virtual thread per task
                - Evidence CSV: `artifacts/logs/week09_concurrency_it_20260506.csv`

                ## Result

                | Executor | Completed tasks | Elapsed ms |
                |---|---:|---:|
                | %s | %d | %d |
                | %s | %d | %d |

                ## Interpretation

                This result only supports one narrow claim:
                virtual threads are more efficient for many concurrent blocking waits than a small fixed
                platform-thread pool in this local experiment.

                It does not prove that virtual threads improve CPU-bound work.
                It does not replace JFR/JMC analysis.
                It should be treated as the Week09 entry point for JVM observability and concurrency experiments.
                """.formatted(
                Instant.now(),
                TASKS,
                BLOCKING_SLEEP_MS,
                PLATFORM_THREADS,
                platform.label(), platform.completedTasks(), platform.elapsedMillis(),
                virtual.label(), virtual.completedTasks(), virtual.elapsedMillis()
        ));
    }

    private void appendRow(StringBuilder text, Result result, String notes) {
        double throughput = result.elapsedMillis() == 0
                ? 0.0
                : result.completedTasks() * 1000.0 / result.elapsedMillis();

        text.append("2026-05-06,")
                .append(result.label()).append(",")
                .append(TASKS).append(",")
                .append(BLOCKING_SLEEP_MS).append(",")
                .append(result.elapsedMillis()).append(",")
                .append(String.format("%.4f", throughput)).append(",")
                .append(notes).append("\n");
    }

    private record Result(String label, int completedTasks, long elapsedMillis) {
    }
}
