package com.ryan.media.repair;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RepairWorkflowController {
    static final String DEFAULT_HANDOFF =
            "artifacts/manifests/repair_handoff_20260715.json";

    private final RepairWorkflowService service;

    public RepairWorkflowController(RepairWorkflowService service) {
        this.service = service;
    }

    @PostMapping("/repair-workflows/import")
    public RepairWorkflowService.ImportResult importHandoff(
            @RequestBody(required = false) ImportRequest request) {
        String path = request == null || request.handoffPath() == null
                ? DEFAULT_HANDOFF
                : request.handoffPath();
        return service.importHandoff(path);
    }

    @GetMapping("/repair-workflows/{batchId}")
    public RepairWorkflowService.BatchSnapshot batch(@PathVariable String batchId) {
        return service.getBatch(batchId);
    }

    @GetMapping("/repair-workflows/{batchId}/summary")
    public RepairWorkflowService.WorkflowSummary summary(@PathVariable String batchId) {
        return service.summary(batchId);
    }

    @GetMapping("/repair-records")
    public List<RepairResultCard> records() {
        return service.listRecords();
    }

    @GetMapping("/repair-records/{repairId}")
    public ResponseEntity<RepairResultCard> record(@PathVariable String repairId) {
        RepairResultCard card = service.getRecord(repairId);
        return ResponseEntity.ok()
                .eTag(etag(card.repairId(), card.reviewVersion()))
                .body(card);
    }

    @PostMapping("/repair-records/{repairId}/reviews")
    public ResponseEntity<RepairResultCard> review(
            @PathVariable String repairId,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestBody RepairReviewRequest request) {
        RepairResultCard current = service.getRecord(repairId);
        if (ifMatch != null && !ifMatch.equals(etag(repairId, current.reviewVersion()))) {
            throw new ReviewVersionConflictException(repairId, current.reviewVersion());
        }
        RepairResultCard updated = service.submitReview(repairId, request);
        return ResponseEntity.ok()
                .eTag(etag(updated.repairId(), updated.reviewVersion()))
                .body(updated);
    }

    @GetMapping("/repair-records/{repairId}/history")
    public List<RepairRecord.ReviewEvent> history(@PathVariable String repairId) {
        return service.history(repairId);
    }

    static String etag(String repairId, long version) {
        return "\"repair-" + repairId + "-v" + version + "\"";
    }

    public record ImportRequest(String handoffPath) {
    }
}
