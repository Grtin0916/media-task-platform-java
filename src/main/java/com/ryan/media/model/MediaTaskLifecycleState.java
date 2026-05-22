package com.ryan.media.model;

/**
 * Week12 minimal media-task lifecycle states.
 *
 * Boundary:
 * - This enum models local task orchestration semantics only.
 * - It does not claim distributed worker execution, durable queue semantics, retry policy,
 *   database persistence, or exactly-once processing.
 */
public enum MediaTaskLifecycleState {
    CREATED,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}