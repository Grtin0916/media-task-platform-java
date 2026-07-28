package com.ryan.media.demo;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
@ActiveProfiles("demo-job-it")
@SpringBootTest(classes=DemoJobTestApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties={"demo.runner.mainbase-root=../audio_engineering_repo_skeleton_v1","demo.runner.java-root=.",
 "demo.runner.handoff=artifacts/manifests/dss_rerank_repair_handoff_20260722.json",
 "demo.runner.script=../media-task-platform-java/artifacts/fixtures/demo-job/sleep_runner.py",
 "demo.runner.config=../media-task-platform-java/artifacts/fixtures/demo-job/fake_config.json",
 "debug=false","logging.level.root=INFO"})
class DemoJobFailureControllerIT {
    @Autowired TestRestTemplate http;
    @Test void timeoutRetryAndCancelCreateImmutableAttempts()throws Exception{
        String timed=create("timeout-job",1);await(timed,"TIMED_OUT",Duration.ofSeconds(8));
        http.postForEntity("/api/demo-jobs/"+timed+"/retry",null,JsonNode.class);
        await(timed,"FAILED",Duration.ofSeconds(10));
        JsonNode attempts=http.getForObject("/api/demo-jobs/"+timed+"/attempts",JsonNode.class);
        assertEquals(2,attempts.size());assertEquals("TIMEOUT",attempts.get(0).path("status").asText());
        assertEquals("FAILED",attempts.get(1).path("status").asText());
        String cancelled=create("cancel-job",120);await(cancelled,"RUNNING",Duration.ofSeconds(5));
        JsonNode cancel=http.postForObject("/api/demo-jobs/"+cancelled+"/cancel",null,JsonNode.class);
        assertEquals("CANCELLED",cancel.path("executionStatus").asText());
    }
    private String create(String key,int timeout){
        HttpHeaders h=new HttpHeaders();h.set("Idempotency-Key",key);
        Map<String,Object>b=Map.of("caseId","fb_001_tuesday_repair","mode","REPLAY","timeoutSeconds",timeout,"resume",true);
        return http.exchange("/api/demo-jobs",HttpMethod.POST,new HttpEntity<>(b,h),JsonNode.class).getBody().path("jobId").asText();
    }
    private JsonNode await(String id,String status,Duration timeout)throws Exception{
        Instant deadline=Instant.now().plus(timeout);JsonNode x;
        do{x=http.getForObject("/api/demo-jobs/"+id,JsonNode.class);if(status.equals(x.path("executionStatus").asText()))return x;Thread.sleep(50);}while(Instant.now().isBefore(deadline));
        fail("job did not reach "+status);return null;
    }
}
