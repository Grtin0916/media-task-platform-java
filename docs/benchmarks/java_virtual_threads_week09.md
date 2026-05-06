# Week09 Java Virtual Threads and JVM Metrics Baseline

## 1. Scope

This document records the Week09 Java observability and virtual-thread baseline for `media-task-platform-java`.

The goal is narrow:

- verify that Actuator / Prometheus can expose JVM and application metrics;
- verify that Java 21 virtual threads can be compared against a fixed platform-thread pool under a blocking workload;
- keep the conclusion bounded to local evidence.

This document does not claim production-grade JVM profiling, distributed tracing, or CPU-bound optimization.

---

## 2. Evidence Inputs

### 2.1 Actuator / Prometheus metrics

Evidence files:

- `artifacts/logs/week09_java_entry_probe_20260506.log`
- `artifacts/logs/week09_java_app_actuator_20260506.log`
- `artifacts/logs/week09_actuator_health_20260506.json`
- `artifacts/logs/week09_actuator_prometheus_20260506.txt`
- `artifacts/logs/week09_actuator_metrics_http_code_20260506.log`

Observed result:

- `/actuator/health` returned `UP`;
- `/actuator/prometheus` exposed JVM, HTTP, HikariCP and JDBC metrics;
- `/actuator/metrics` returned `404` because the current exposure config only includes `health,info,prometheus`.

Current exposure config:

- `management.endpoints.web.exposure.include=health,info,prometheus`

This is a configuration boundary, not a runtime failure.

### 2.2 Virtual-thread concurrency experiment

Evidence files:

- `src/test/java/com/ryan/media/ConcurrencyIT.java`
- `artifacts/logs/week09_concurrency_it_20260506.csv`
- `artifacts/logs/week09_concurrency_it_20260506.md`
- `artifacts/logs/week09_concurrency_it_test_20260506.log`

Test command:

    ./mvnw -Dtest=ConcurrencyIT test

Observed Maven result:

- Tests run: 1
- Failures: 0
- Errors: 0
- BUILD SUCCESS

---

## 3. Concurrency Result

| Executor | Tasks | Blocking sleep ms | Elapsed ms | Throughput tasks/sec |
|---|---:|---:|---:|---:|
| platform-fixed-8 | 160 | 40 | 806 | 198.5112 |
| virtual-thread-per-task | 160 | 40 | 59 | 2711.8644 |

The virtual-thread executor completed the blocking workload faster than the small fixed platform-thread pool in this local experiment.

---

## 4. Interpretation

The result supports one narrow claim:

Java 21 virtual threads are a better fit than a small fixed platform-thread pool for many concurrent blocking waits in this controlled local workload.

The result does not support these broader claims:

- virtual threads are always faster;
- virtual threads improve CPU-bound workloads;
- this test replaces JFR, JMC, GC analysis, or production profiling;
- this result is enough to make a production threading decision.

The practical engineering takeaway is:

- use virtual threads as a candidate for blocking I/O style workloads;
- keep platform threads and bounded pools for CPU-bound work or cases requiring strict resource control;
- use JFR / jcmd evidence before making broader runtime conclusions.

---

## 5. Week09 Status

Completed:

- Actuator / Prometheus JVM metrics baseline;
- local blocking workload comparison between fixed platform threads and virtual threads;
- CSV, Markdown, and Maven test log evidence.

Deferred:

- JFR recording and event interpretation;
- jcmd thread / VM summary capture;
- custom Micrometer business metrics;
- tracing / distributed span evidence;
- production-grade load testing.

---

## 6. JFR Minimal Recording

### 6.1 Scope

This section records a minimal Java Flight Recorder run for the Week09 `ConcurrencyIT` baseline.

The first attempt used Surefire forked JVM `argLine` with `delay=0s`, which failed before test execution because JFR requires the startup delay to be at least 1 second. The successful path records the Maven JVM through `MAVEN_OPTS`, while running `ConcurrencyIT`.

Evidence files:

- `artifacts/jfr/week09_concurrency_it_maven_jvm_20260506.jfr`
- `artifacts/logs/week09_concurrency_it_jfr_maven_jvm_20260506.log`
- `artifacts/logs/week09_concurrency_it_jfr_summary_20260506.log`
- `artifacts/logs/week09_concurrency_it_jfr_hot_methods_20260506.log`

Command shape:

    MAVEN_OPTS="-XX:StartFlightRecording=filename=artifacts/jfr/week09_concurrency_it_maven_jvm_20260506.jfr,settings=profile,dumponexit=true,disk=true" ./mvnw -Dtest=ConcurrencyIT test

### 6.2 Observed Result

- JFR file size: approximately `779K`
- Recording duration: approximately `3 s`
- Maven result: `BUILD SUCCESS`
- Test result: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`
- Representative JFR events:
  - `jdk.GCPhaseParallel`
  - `jdk.ObjectAllocationSample`
  - `jdk.ExecutionSample`
  - `jdk.ThreadPark`
  - `jdk.ThreadStart`
  - `jdk.CPULoad`

### 6.3 Interpretation Boundary

This JFR evidence proves that the project can capture and preserve JVM runtime evidence during the Week09 concurrency test path.

The current recording is still a minimal entry point:

- it records the Maven test process rather than a long-running production service;
- hot-method samples are dominated by Maven / test harness activity;
- virtual-thread-specific JFR events are not yet materially populated in this short run;
- it does not replace longer JFR/JMC analysis under a service workload.

The next JVM profiling step should use either a longer-running workload or a running Spring Boot process attached through `jcmd JFR.start`, then inspect virtual-thread, blocking, allocation, GC, and method hotspot events.

