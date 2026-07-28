package com.ryan.media.demo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
public final class DemoJob {
    private final String jobId, idempotencyKey, requestFingerprint, caseId, mode;
    private final String sourceHandoffDigest, sourceCommit, runnerConfigDigest, runnerImplementationVersion;
    private final DemoPublishDecision publishDecision;
    private final Instant createdAt;
    private DemoJobStatus executionStatus;
    private String currentAttemptId;
    private long version;
    private Instant updatedAt;
    private final List<DemoJobAttempt> attempts = new ArrayList<>();
    DemoJob(String jobId, String key, String fingerprint, DemoJobRequest request,
            MainbaseDemoHandoff handoff, DemoPublishDecision publish, String configDigest, String implementation) {
        this.jobId=jobId;idempotencyKey=key;requestFingerprint=fingerprint;caseId=request.caseId();mode=request.mode();
        sourceHandoffDigest=handoff.handoffDigest();sourceCommit=handoff.sourceCommit();
        runnerConfigDigest=configDigest;runnerImplementationVersion=implementation;publishDecision=publish;
        executionStatus=DemoJobStatus.ACCEPTED;createdAt=Instant.now();updatedAt=createdAt;
    }
    public synchronized void status(DemoJobStatus next) { executionStatus=next;version++;updatedAt=Instant.now(); }
    public synchronized void currentAttempt(String id) { currentAttemptId=id;version++;updatedAt=Instant.now(); }
    public synchronized void addAttempt(DemoJobAttempt attempt) { attempts.add(attempt);version++;updatedAt=Instant.now(); }
    public String getJobId(){return jobId;} public String getIdempotencyKey(){return idempotencyKey;}
    public String getRequestFingerprint(){return requestFingerprint;} public String getCaseId(){return caseId;}
    public String getMode(){return mode;} public String getSourceHandoffDigest(){return sourceHandoffDigest;}
    public String getSourceCommit(){return sourceCommit;} public String getRunnerConfigDigest(){return runnerConfigDigest;}
    public String getRunnerImplementationVersion(){return runnerImplementationVersion;}
    public synchronized DemoJobStatus getExecutionStatus(){return executionStatus;}
    public DemoPublishDecision getPublishDecision(){return publishDecision;}
    public synchronized String getCurrentAttemptId(){return currentAttemptId;}
    public synchronized long getVersion(){return version;} public Instant getCreatedAt(){return createdAt;}
    public synchronized Instant getUpdatedAt(){return updatedAt;}
    public synchronized List<DemoJobAttempt> getAttempts(){return List.copyOf(attempts);}
}
