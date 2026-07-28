package com.ryan.media.demo;
import java.util.EnumSet;
import java.util.Map;
public final class DemoJobStateMachine {
    private static final Map<DemoJobStatus, EnumSet<DemoJobStatus>> ALLOWED = Map.ofEntries(
            Map.entry(DemoJobStatus.ACCEPTED, EnumSet.of(DemoJobStatus.QUEUED, DemoJobStatus.BLOCKED)),
            Map.entry(DemoJobStatus.QUEUED, EnumSet.of(DemoJobStatus.STARTING, DemoJobStatus.CANCEL_REQUESTED)),
            Map.entry(DemoJobStatus.STARTING, EnumSet.of(DemoJobStatus.RUNNING, DemoJobStatus.FAILED, DemoJobStatus.CANCELLED)),
            Map.entry(DemoJobStatus.RUNNING, EnumSet.of(DemoJobStatus.FINALIZING, DemoJobStatus.TIMED_OUT, DemoJobStatus.CANCEL_REQUESTED, DemoJobStatus.FAILED)),
            Map.entry(DemoJobStatus.FINALIZING, EnumSet.of(DemoJobStatus.SUCCEEDED, DemoJobStatus.FAILED)),
            Map.entry(DemoJobStatus.CANCEL_REQUESTED, EnumSet.of(DemoJobStatus.CANCELLED, DemoJobStatus.FAILED)),
            Map.entry(DemoJobStatus.FAILED, EnumSet.of(DemoJobStatus.QUEUED)),
            Map.entry(DemoJobStatus.TIMED_OUT, EnumSet.of(DemoJobStatus.QUEUED)),
            Map.entry(DemoJobStatus.CANCELLED, EnumSet.of(DemoJobStatus.QUEUED)));
    public void transition(DemoJob job, DemoJobStatus next) {
        DemoJobStatus current=job.getExecutionStatus();
        if (!ALLOWED.getOrDefault(current, EnumSet.noneOf(DemoJobStatus.class)).contains(next))
            throw new DemoJobException("INVALID_JOB_TRANSITION", current+" -> "+next);
        job.status(next);
    }
}
