package com.ryan.media.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

@Service
public class DemoJobExecutionService {
    private final MainbaseDemoHandoff handoff;
    private final Map<String,DemoResultSeed> seeds;
    private final DemoJobRepository repository;private final DemoRunnerProperties properties;
    private final DemoRunnerCommandFactory commands;private final LocalRunnerProcess runner;
    private final DemoJobFinalizer finalizer;private final DemoJobEvidenceWriter evidence;
    private final DemoJobStateMachine states=new DemoJobStateMachine();
    private final ExecutorService executor;
    private final Map<String,LocalRunnerProcess.Running> active=new ConcurrentHashMap<>();

    public DemoJobExecutionService(MainbaseDemoHandoffImporter importer,DemoJobRepository repository,
            DemoRunnerProperties properties,DemoRunnerCommandFactory commands,LocalRunnerProcess runner,
            DemoJobFinalizer finalizer,DemoJobEvidenceWriter evidence) {
        this.handoff=importer.importHandoff();this.repository=repository;this.properties=properties;
        this.commands=commands;this.runner=runner;this.finalizer=finalizer;this.evidence=evidence;
        this.seeds=handoff.records().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(DemoResultSeed::caseId,x->x));
        this.executor=Executors.newFixedThreadPool(properties.maxConcurrentJobs());
        evidence.write(handoff,List.of());
    }
    public synchronized DemoJob create(String key,DemoJobRequest request) {
        if(key==null||key.isBlank())throw new DemoJobException("IDEMPOTENCY_KEY_REQUIRED","Idempotency-Key is required");
        DemoResultSeed seed=requireSeed(request.caseId());
        String fingerprint=fingerprint(request);
        DemoJob existing=repository.byKey(key);
        if(existing!=null){
            if(!existing.getRequestFingerprint().equals(fingerprint))throw new DemoJobException("IDEMPOTENCY_CONFLICT",key);
            return existing;
        }
        DemoJob job=new DemoJob("job-"+UUID.randomUUID(),key,fingerprint,request,handoff,seed.publishDecision(),
                digestConfig(),"demo-job-orchestrator-v1");
        repository.save(job);
        if(!"REPLAY".equals(request.mode())||seed.publishDecision()!=DemoPublishDecision.PROVISIONAL_SELECTED){
            states.transition(job,DemoJobStatus.BLOCKED);evidence();return job;
        }
        states.transition(job,DemoJobStatus.QUEUED);evidence();executor.submit(()->execute(job,seed,request));return job;
    }
    private void execute(DemoJob job,DemoResultSeed seed,DemoJobRequest request) {
        int number=job.getAttempts().size()+1;String attemptId="attempt-%03d".formatted(number);
        job.currentAttempt(attemptId);states.transition(job,DemoJobStatus.STARTING);
        List<String> command=commands.command(seed);String commandDigest=hash(String.join("\u0000",command));
        Path dir=properties.javaRoot().resolve("artifacts/runtime/demo-jobs").resolve(job.getJobId()).resolve(attemptId);
        LocalRunnerProcess.Running running=null;Instant started=Instant.now();
        try {
            running=runner.start(command,properties.mainbaseRoot(),dir);active.put(job.getJobId(),running);
            states.transition(job,DemoJobStatus.RUNNING);
            var outcome=runner.await(running,Duration.ofSeconds(request.timeoutSeconds()));
            if(job.getExecutionStatus()==DemoJobStatus.CANCEL_REQUESTED
                    ||job.getExecutionStatus()==DemoJobStatus.CANCELLED){
                job.status(DemoJobStatus.CANCELLED);
                add(job,attemptId,number,"CANCELLED",commandDigest,running,started,outcome.exitCode(),null,null,
                        "CANCELLED","cancelled by request",outcome.termination(),List.of());return;
            }
            if(outcome.timedOut()){
                states.transition(job,DemoJobStatus.TIMED_OUT);
                add(job,attemptId,number,"TIMEOUT",commandDigest,running,started,null,null,null,"PROCESS_TIMEOUT","timeout",outcome.termination(),List.of());return;
            }
            if(outcome.exitCode()!=0){
                states.transition(job,DemoJobStatus.FAILED);
                add(job,attemptId,number,"FAILED",commandDigest,running,started,outcome.exitCode(),null,null,"RUNNER_EXIT_NONZERO","exit "+outcome.exitCode(),outcome.termination(),List.of());return;
            }
            states.transition(job,DemoJobStatus.FINALIZING);
            var result=finalizer.finalizeResult(running);states.transition(job,DemoJobStatus.SUCCEEDED);
            add(job,attemptId,number,"SUCCEEDED",commandDigest,running,started,0,result.manifestPath(),result.manifestDigest(),"","",outcome.termination(),result.stages(),result.resultDigest());
        } catch(DemoJobException e){
            if(job.getExecutionStatus()!=DemoJobStatus.CANCELLED&&job.getExecutionStatus()!=DemoJobStatus.TIMED_OUT)job.status(DemoJobStatus.FAILED);
            if(running!=null)add(job,attemptId,number,"FAILED",commandDigest,running,started,null,null,null,e.code(),e.getMessage(),new DemoProcessTreeTerminator.TerminationResult(0,0,0),List.of());
        } finally {active.remove(job.getJobId());evidence();}
    }
    private void add(DemoJob job,String id,int number,String status,String commandDigest,LocalRunnerProcess.Running running,
            Instant started,Integer exit,String manifest,String manifestDigest,String failureCode,String reason,
            DemoProcessTreeTerminator.TerminationResult termination,List<DemoJobStageSnapshot> stages) {
        add(job,id,number,status,commandDigest,running,started,exit,manifest,manifestDigest,failureCode,reason,termination,stages,manifestDigest);
    }
    private void add(DemoJob job,String id,int number,String status,String commandDigest,LocalRunnerProcess.Running running,
            Instant started,Integer exit,String manifest,String manifestDigest,String failureCode,String reason,
            DemoProcessTreeTerminator.TerminationResult termination,List<DemoJobStageSnapshot> stages,String resultDigest) {
        Instant end=Instant.now();job.addAttempt(new DemoJobAttempt(id,number,status,commandDigest,
                properties.mainbaseRoot().toString(),running.process().pid(),started,end,
                Duration.between(started,end).toMillis(),exit,rel(running.stdout()),rel(running.stderr()),
                manifest,manifestDigest,resultDigest,failureCode,reason,termination.discovered(),termination.terminated(),
                termination.stillAlive(),stages));
    }
    public synchronized DemoJob retry(String id) {
        DemoJob job=repository.require(id);
        if(!java.util.Set.of(DemoJobStatus.FAILED,DemoJobStatus.TIMED_OUT,DemoJobStatus.CANCELLED).contains(job.getExecutionStatus()))
            throw new DemoJobException("RETRY_NOT_ALLOWED",job.getExecutionStatus().name());
        states.transition(job,DemoJobStatus.QUEUED);
        DemoJobRequest request=new DemoJobRequest(job.getCaseId(),job.getMode(),120,true);
        executor.submit(()->execute(job,requireSeed(job.getCaseId()),request));return job;
    }
    public synchronized DemoJob cancel(String id) {
        DemoJob job=repository.require(id);LocalRunnerProcess.Running running=active.get(id);
        if(running==null)throw new DemoJobException("CANCEL_NOT_ALLOWED",job.getExecutionStatus().name());
        states.transition(job,DemoJobStatus.CANCEL_REQUESTED);
        runner.cancel(running,properties.cancelGrace());job.status(DemoJobStatus.CANCELLED);evidence();return job;
    }
    public DemoJob require(String id){return repository.require(id);}
    public List<DemoJobAttempt> attempts(String id){return repository.require(id).getAttempts();}
    public DemoJobResultCard result(String id){
        DemoJob j=require(id);String digest=j.getAttempts().isEmpty()?"":j.getAttempts().get(j.getAttempts().size()-1).resultDigest();
        DemoResultSeed seed=requireSeed(j.getCaseId());
        return new DemoJobResultCard(j.getJobId(),j.getExecutionStatus(),j.getPublishDecision(),false,true,false,
                seed.liveGroupAvailable(),j.getSourceCommit(),digest,
                j.getExecutionStatus()==DemoJobStatus.SUCCEEDED?"VALID":"NOT_CHECKED");
    }
    public String logs(String id,String stream,int tail) {
        DemoJob j=require(id);if(j.getAttempts().isEmpty())return "";
        DemoJobAttempt a=j.getAttempts().get(j.getAttempts().size()-1);
        String rel="stderr".equals(stream)?a.stderrPath():a.stdoutPath();
        try {List<String> lines=Files.readAllLines(properties.javaRoot().resolve(rel));return String.join("\n",lines.subList(Math.max(0,lines.size()-Math.min(1000,tail)),lines.size()));}
        catch(Exception e){return "";}
    }
    public MainbaseDemoHandoff handoff(){return handoff;}
    private DemoResultSeed requireSeed(String id){DemoResultSeed s=seeds.get(id);if(s==null)throw new DemoJobException("INVALID_CASE_ID",id);return s;}
    private String fingerprint(DemoJobRequest r){return hash(r.caseId()+"|"+r.mode()+"|"+r.timeoutSeconds()+"|"+r.resume()+"|"+handoff.handoffDigest()+"|"+digestConfig()+"|demo-job-orchestrator-v1");}
    private String digestConfig(){Path p=properties.mainbaseRoot().resolve(properties.config());return Files.isRegularFile(p)?MainbaseDemoHandoffImporter.sha256(p):hash(properties.config());}
    private String hash(String s){return MainbaseDemoHandoffImporter.sha256(s.getBytes(StandardCharsets.UTF_8));}
    private String rel(Path p){return properties.javaRoot().relativize(p.toAbsolutePath().normalize()).toString().replace('\\','/');}
    private void evidence(){evidence.write(handoff,new ArrayList<>(repository.all()));}
    @PreDestroy void close(){executor.shutdownNow();}
}
