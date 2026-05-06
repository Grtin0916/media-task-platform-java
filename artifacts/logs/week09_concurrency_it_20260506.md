# Week09 ConcurrencyIT Evidence

## Scope

This test compares a small fixed platform-thread pool with Java 21 virtual threads
under a blocking-wait workload.

## Experiment setup

- Date: 2026-05-06T02:49:45.953249675Z
- Tasks: 160
- Blocking sleep per task: 40 ms
- Platform executor: fixed thread pool, size=8
- Virtual executor: virtual thread per task
- Evidence CSV: `artifacts/logs/week09_concurrency_it_20260506.csv`

## Result

| Executor | Completed tasks | Elapsed ms |
|---|---:|---:|
| platform-fixed-8 | 160 | 806 |
| virtual-thread-per-task | 160 | 59 |

## Interpretation

This result only supports one narrow claim:
virtual threads are more efficient for many concurrent blocking waits than a small fixed
platform-thread pool in this local experiment.

It does not prove that virtual threads improve CPU-bound work.
It does not replace JFR/JMC analysis.
It should be treated as the Week09 entry point for JVM observability and concurrency experiments.
