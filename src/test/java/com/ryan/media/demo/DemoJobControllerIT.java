package com.ryan.media.demo;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
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
 "debug=false","logging.level.root=INFO"})
class DemoJobControllerIT {
    @Autowired TestRestTemplate http;
    @Test void realReplayIdempotencyResultLogsAndBlockedGates()throws Exception{
        HttpHeaders h=new HttpHeaders();h.set("Idempotency-Key","real-replay-1");
        Map<String,Object> body=Map.of("caseId","fb_001_tuesday_repair","mode","REPLAY","timeoutSeconds",120,"resume",true);
        ResponseEntity<JsonNode> created=http.exchange("/api/demo-jobs",HttpMethod.POST,new HttpEntity<>(body,h),JsonNode.class);
        assertEquals(HttpStatus.ACCEPTED,created.getStatusCode());String id=created.getBody().path("jobId").asText();
        JsonNode job=await(id,"SUCCEEDED",Duration.ofSeconds(15));
        assertEquals("PROVISIONAL_SELECTED",job.path("publishDecision").asText());
        JsonNode result=http.getForObject("/api/demo-jobs/"+id+"/result",JsonNode.class);
        assertFalse(result.path("finalSelected").asBoolean());assertTrue(result.path("proxyEvidenceOnly").asBoolean());
        assertEquals("VALID",result.path("integrityStatus").asText());
        JsonNode attempts=http.getForObject("/api/demo-jobs/"+id+"/attempts",JsonNode.class);
        assertEquals(1,attempts.size());assertTrue(attempts.get(0).path("pid").asLong()>0);
        assertEquals("SUCCEEDED",attempts.get(0).path("status").asText());
        String logs=http.getForObject("/api/demo-jobs/"+id+"/logs?tail=50",String.class);assertFalse(logs.isBlank());
        ResponseEntity<JsonNode> same=http.exchange("/api/demo-jobs",HttpMethod.POST,new HttpEntity<>(body,h),JsonNode.class);
        assertEquals(id,same.getBody().path("jobId").asText());
        Map<String,Object> changed=Map.of("caseId","fb_001_tuesday_repair","mode","REPLAY","timeoutSeconds",121,"resume",true);
        ResponseEntity<JsonNode> conflict=http.exchange("/api/demo-jobs",HttpMethod.POST,new HttpEntity<>(changed,h),JsonNode.class);
        assertEquals(HttpStatus.CONFLICT,conflict.getStatusCode());
        assertBlocked("mixed-1",Map.of("caseId","fb_001_tuesday_repair","mode","MIXED","timeoutSeconds",120,"resume",true),"PROVISIONAL_SELECTED");
        assertBlocked("rejected-1",Map.of("caseId","fb_004_transplant_v1","mode","REPLAY","timeoutSeconds",120,"resume",true),"REPAIR_REJECTED");
    }
    private void assertBlocked(String key,Map<String,Object> body,String decision){
        HttpHeaders h=new HttpHeaders();h.set("Idempotency-Key",key);
        JsonNode x=http.exchange("/api/demo-jobs",HttpMethod.POST,new HttpEntity<>(body,h),JsonNode.class).getBody();
        assertEquals("BLOCKED",x.path("executionStatus").asText());assertEquals(decision,x.path("publishDecision").asText());
        assertEquals(0,x.path("attempts").size());
    }
    private JsonNode await(String id,String status,Duration timeout)throws Exception{
        Instant deadline=Instant.now().plus(timeout);JsonNode x;
        do{x=http.getForObject("/api/demo-jobs/"+id,JsonNode.class);if(status.equals(x.path("executionStatus").asText()))return x;Thread.sleep(50);}while(Instant.now().isBefore(deadline));
        fail("job did not reach "+status);return null;
    }
}
