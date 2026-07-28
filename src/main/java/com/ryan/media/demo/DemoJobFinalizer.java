package com.ryan.media.demo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
@Component
public class DemoJobFinalizer {
    private final ObjectMapper mapper;private final DemoRunnerProperties properties;
    public DemoJobFinalizer(ObjectMapper mapper,DemoRunnerProperties properties){this.mapper=mapper;this.properties=properties;}
    public Finalized finalizeResult(LocalRunnerProcess.Running running) {
        try {
            JsonNode output=mapper.readTree(running.stdout().toFile());
            String runId=output.path("run_id").asText("");
            if(runId.isBlank())throw new DemoJobException("RUN_MANIFEST_MISSING","runner did not return run_id");
            Path root=properties.mainbaseRoot().toRealPath();
            Path manifest=root.resolve("artifacts/runs").resolve(runId).resolve("run_manifest.json").normalize();
            if(!manifest.startsWith(root)||!Files.isRegularFile(manifest))
                throw new DemoJobException("RUN_MANIFEST_MISSING",manifest.toString());
            Path real=manifest.toRealPath();
            if(!real.startsWith(root)||!Files.isRegularFile(real))throw new DemoJobException("RUN_MANIFEST_MISSING",manifest.toString());
            JsonNode node=mapper.readTree(real.toFile());
            if(!"SUCCEEDED".equals(node.path("status").asText()))throw new DemoJobException("FAILED_FINALIZATION","manifest status is not SUCCEEDED");
            validateOutputDigests(root,node);
            List<DemoJobStageSnapshot> stages=new ArrayList<>();
            for(JsonNode s:node.path("stages"))stages.add(new DemoJobStageSnapshot(
                    s.path("stage_id").asText(),s.path("status").asText(),s.path("execution_mode").asText(),
                    s.path("reused").asBoolean(),mapper.convertValue(s.path("input_digests"),java.util.Map.class),
                    mapper.convertValue(s.path("output_digests"),java.util.Map.class),s.path("duration_ms").asLong(),
                    s.path("failure").isNull()?"":s.path("failure").asText()));
            String digest=MainbaseDemoHandoffImporter.sha256(real);
            return new Finalized(properties.mainbaseRoot().relativize(real).toString().replace('\\','/'),
                    digest,digestValue(node),List.copyOf(stages));
        } catch(DemoJobException e){throw e;}
        catch(Exception e){throw new DemoJobException("FAILED_FINALIZATION",e.getMessage());}
    }
    private void validateOutputDigests(Path root,JsonNode manifest) {
        for(JsonNode stage:manifest.path("stages")){
            var fields=stage.path("output_digests").fields();
            while(fields.hasNext()){
                var output=fields.next();
                try {
                    Path candidate=root.resolve(output.getKey()).normalize();
                    Path real=candidate.toRealPath();
                    if(!real.startsWith(root)||!Files.isRegularFile(real))
                        throw new DemoJobException("RESULT_DIGEST_MISMATCH",output.getKey());
                    String expected=output.getValue().asText();
                    String actual="sha256:"+MainbaseDemoHandoffImporter.sha256(real);
                    if(!actual.equals(expected))
                        throw new DemoJobException("RESULT_DIGEST_MISMATCH",output.getKey());
                } catch(DemoJobException e){throw e;}
                catch(Exception e){throw new DemoJobException("RESULT_DIGEST_MISMATCH",output.getKey());}
            }
        }
    }
    private String digestValue(JsonNode node) {
        try{return MainbaseDemoHandoffImporter.sha256(mapper.writeValueAsBytes(node));}
        catch(Exception e){throw new DemoJobException("FAILED_FINALIZATION",e.getMessage());}
    }
    public record Finalized(String manifestPath,String manifestDigest,String resultDigest,List<DemoJobStageSnapshot> stages){}
}
