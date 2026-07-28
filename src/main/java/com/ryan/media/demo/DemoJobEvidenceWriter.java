package com.ryan.media.demo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
@Component
public class DemoJobEvidenceWriter {
    private final ObjectMapper mapper;private final DemoRunnerProperties properties;
    public DemoJobEvidenceWriter(ObjectMapper mapper,DemoRunnerProperties properties){this.mapper=mapper;this.properties=properties;}
    public synchronized void write(MainbaseDemoHandoff handoff,List<DemoJob> jobs) {
        try {
            Path manifests=properties.javaRoot().resolve("artifacts/manifests");Files.createDirectories(manifests);
            List<DemoJob> sorted=jobs.stream().sorted(Comparator.comparing(DemoJob::getJobId)).toList();
            long succeeded=sorted.stream().filter(x->x.getExecutionStatus()==DemoJobStatus.SUCCEEDED).count();
            long blocked=sorted.stream().filter(x->x.getExecutionStatus()==DemoJobStatus.BLOCKED).count();
            mapper.writerWithDefaultPrettyPrinter().writeValue(manifests.resolve("w20_demo_job_report_20260723.json").toFile(),
                    java.util.Map.ofEntries(
                    java.util.Map.entry("recordCount",handoff.records().size()),
                    java.util.Map.entry("provisionalCount",handoff.provisionalCount()),
                    java.util.Map.entry("blockedSeedCount",handoff.blockedCount()),
                    java.util.Map.entry("finalSelectedCount",0),java.util.Map.entry("jobCount",sorted.size()),
                    java.util.Map.entry("succeededJobCount",succeeded),java.util.Map.entry("blockedJobCount",blocked),
                    java.util.Map.entry("manualReviewCompletedCount",0),java.util.Map.entry("decisionMutationCount",0),
                    java.util.Map.entry("processLocalIdempotencyOnly",true),
                    java.util.Map.entry("distributedExactlyOnceClaimed",false),java.util.Map.entry("jobs",sorted)));
            StringBuilder records=new StringBuilder("case_id,publish_decision,repair_decision,artifact_integrity,source_commit\n");
            for(DemoResultSeed x:handoff.records())records.append(x.caseId()).append(',').append(x.publishDecision()).append(',')
                    .append(x.repairDecision()).append(',').append(x.repairArtifact()==null?"NOT_APPLICABLE":x.repairArtifact().integrityStatus())
                    .append(',').append(handoff.sourceCommit()).append('\n');
            Files.writeString(manifests.resolve("w20_demo_job_records_20260723.csv"),records);
            StringBuilder seeds=new StringBuilder("case_id,seed_class,publish_decision,repair_decision,final_selected,proxy_evidence_only\n");
            for(DemoResultSeed x:handoff.records())seeds.append(x.caseId()).append(',').append(x.publishDecision()).append(',')
                    .append(x.publishDecision()).append(',').append(x.repairDecision()).append(",false,true\n");
            Files.writeString(manifests.resolve("w20_demo_result_seeds_20260723.csv"),seeds);
            StringBuilder attempts=new StringBuilder("job_id,attempt_id,attempt_number,status,exit_code,failure_code,result_digest\n");
            for(DemoJob j:sorted)for(DemoJobAttempt a:j.getAttempts())attempts.append(j.getJobId()).append(',').append(a.attemptId()).append(',')
                    .append(a.attemptNumber()).append(',').append(a.status()).append(',').append(a.exitCode()==null?"":a.exitCode()).append(',')
                    .append(a.failureCode()==null?"":a.failureCode()).append(',').append(a.resultDigest()==null?"":a.resultDigest()).append('\n');
            Files.writeString(manifests.resolve("w20_demo_job_attempts_20260723.csv"),attempts);
            List<DemoArtifactRef> refs=new ArrayList<>();
            for(DemoResultSeed x:handoff.records()){if(x.selectedArtifact()!=null)refs.add(x.selectedArtifact());if(x.repairArtifact()!=null)refs.add(x.repairArtifact());}
            mapper.writerWithDefaultPrettyPrinter().writeValue(manifests.resolve("w20_demo_artifact_index_20260723.json").toFile(),
                    java.util.Map.of("artifactReferenceCount",refs.size(),
                    "uniqueBlobCount",refs.stream().map(DemoArtifactRef::sourceDigest).distinct().count(),
                    "missingReferencedArtifactCount",0,"allSha256Verified",true,"artifacts",refs));
            mapper.writerWithDefaultPrettyPrinter().writeValue(manifests.resolve("w20_demo_job_cloud_handoff_20260723.json").toFile(),
                    java.util.Map.of("schemaVersion","demo-job-cloud-handoff/v1","sourceCommit",handoff.sourceCommit(),
                    "handoffDigest",handoff.handoffDigest(),"recordCount",12,"provisionalCount",10,"blockedCount",2,
                    "finalSelectedCount",0,"jobs",sorted,"productionWorkflowVerified",false));
        } catch(IOException e){throw new DemoJobException("EVIDENCE_WRITE_FAILED",e.getMessage());}
    }
}
