package com.ryan.media.demo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/demo-jobs")
public class DemoJobController {
    private final DemoJobExecutionService service;
    public DemoJobController(DemoJobExecutionService service){this.service=service;}
    @PostMapping
    ResponseEntity<DemoJob> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody DemoJobRequest request){
        DemoJob job=service.create(key,request);
        return ResponseEntity.status(job.getExecutionStatus()==DemoJobStatus.BLOCKED?HttpStatus.OK:HttpStatus.ACCEPTED)
                .eTag(etag(job)).body(job);
    }
    @GetMapping("/{id}") ResponseEntity<DemoJob> get(@PathVariable String id){
        DemoJob job=service.require(id);return ResponseEntity.ok().eTag(etag(job)).body(job);
    }
    @GetMapping("/{id}/attempts") List<DemoJobAttempt> attempts(@PathVariable String id){return service.attempts(id);}
    @GetMapping("/{id}/result") DemoJobResultCard result(@PathVariable String id){return service.result(id);}
    @GetMapping("/{id}/logs") String logs(@PathVariable String id,@RequestParam(defaultValue="stdout")String stream,@RequestParam(defaultValue="200")int tail){return service.logs(id,stream,tail);}
    @PostMapping("/{id}/cancel") DemoJob cancel(@PathVariable String id){return service.cancel(id);}
    @PostMapping("/{id}/retry") DemoJob retry(@PathVariable String id){return service.retry(id);}
    private String etag(DemoJob job){return "\"job-"+job.getJobId()+"-v"+job.getVersion()+"\"";}
}
