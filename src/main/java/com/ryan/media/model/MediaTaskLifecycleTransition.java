package com.ryan.media.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Week12 V0 lifecycle transition guard for media tasks.
 *
 * This is intentionally small and deterministic:
 * CREATED -> QUEUED / FAILED
 * QUEUED -> RUNNING / FAILED
 * RUNNING -> SUCCEEDED / FAILED
 * SUCCEEDED -> terminal
 * FAILED -> terminal
 *
 * The goal is to pin the business contract before wiring persistence, workers, events, or retries.
 */
public final class MediaTaskLifecycleTransition {

    private static final EnumMap<MediaTaskLifecycleState, EnumSet<MediaTaskLifecycleState>> ALLOWED =
            new EnumMap<>(MediaTaskLifecycleState.class);

    static {
        ALLOWED.put(MediaTaskLifecycleState.CREATED, EnumSet.of(
                MediaTaskLifecycleState.QUEUED,
                MediaTaskLifecycleState.FAILED
        ));
        ALLOWED.put(MediaTaskLifecycleState.QUEUED, EnumSet.of(
                MediaTaskLifecycleState.RUNNING,
                MediaTaskLifecycleState.FAILED
        ));
        ALLOWED.put(MediaTaskLifecycleState.RUNNING, EnumSet.of(
                MediaTaskLifecycleState.SUCCEEDED,
                MediaTaskLifecycleState.FAILED
        ));
        ALLOWED.put(MediaTaskLifecycleState.SUCCEEDED, EnumSet.noneOf(MediaTaskLifecycleState.class));
        ALLOWED.put(MediaTaskLifecycleState.FAILED, EnumSet.noneOf(MediaTaskLifecycleState.class));
    }

    private MediaTaskLifecycleTransition() {
    }

    public static boolean canTransition(MediaTaskLifecycleState from, MediaTaskLifecycleState to) {
        Objects.requireNonNull(from, "from state must not be null");
        Objects.requireNonNull(to, "to state must not be null");
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(MediaTaskLifecycleState.class)).contains(to);
    }

    public static void requireTransition(MediaTaskLifecycleState from, MediaTaskLifecycleState to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Illegal media task lifecycle transition: " + from + " -> " + to);
        }
    }

    public static Set<MediaTaskLifecycleState> allowedNextStates(MediaTaskLifecycleState from) {
        Objects.requireNonNull(from, "from state must not be null");
        return EnumSet.copyOf(ALLOWED.getOrDefault(from, EnumSet.noneOf(MediaTaskLifecycleState.class)));
    }

    public static boolean isTerminal(MediaTaskLifecycleState state) {
        Objects.requireNonNull(state, "state must not be null");
        return state == MediaTaskLifecycleState.SUCCEEDED || state == MediaTaskLifecycleState.FAILED;
    }

    public static boolean requiresEvidenceLinks(MediaTaskLifecycleState state) {
        Objects.requireNonNull(state, "state must not be null");
        return state == MediaTaskLifecycleState.SUCCEEDED;
    }
}