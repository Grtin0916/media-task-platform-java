package com.ryan.media.repair;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepairWorkflowService {
    private final RepairImportService importService;
    private final ObjectMapper objectMapper;
    private final Path javaRoot;
    private final Map<String, RepairRecord> records = new LinkedHashMap<>();
    private final Map<String, BatchSnapshot> batches = new LinkedHashMap<>();

    public RepairWorkflowService(
            RepairImportService importService,
            ObjectMapper objectMapper,
            @Value("${repair.java-root:.}") String javaRoot) {
        this.importService = importService;
        this.objectMapper = objectMapper;
        this.javaRoot = Path.of(javaRoot).toAbsolutePath().normalize();
    }

    public synchronized ImportResult importHandoff(String handoffRelativePath) {
        RepairImportService.PreparedBatch prepared = importService.prepare(handoffRelativePath);
        BatchSnapshot existing = batches.get(prepared.batchId());
        if (existing != null) {
            return new ImportResult(prepared.batchId(), existing.repairIds().size(), true);
        }
        List<RepairRecord> incoming = prepared.records().stream()
                .map(row -> new RepairRecord(
                        row.repairId(),
                        row.parentCandidateId(),
                        row.failureId(),
                        row.repairAction(),
                        row.sourceMode(),
                        row.metrics(),
                        row.reason(),
                        row.beforeArtifact(),
                        row.afterArtifact(),
                        prepared.batchId(),
                        prepared.sourceCommit(),
                        row.repairDecision()))
                .toList();
        if (incoming.stream().anyMatch(row -> records.containsKey(row.repairId()))) {
            throw new InvalidRepairRequestException(
                    "repairId collision across different import batches");
        }
        incoming.forEach(row -> records.put(row.repairId(), row));
        BatchSnapshot snapshot = new BatchSnapshot(
                prepared.batchId(),
                prepared.sourceCommit(),
                incoming.stream().map(RepairRecord::repairId).toList(),
                prepared.importedAt());
        batches.put(prepared.batchId(), snapshot);
        writeEvidence(prepared, incoming);
        return new ImportResult(prepared.batchId(), incoming.size(), false);
    }

    public synchronized List<RepairResultCard> listRecords() {
        return records.values().stream()
                .map(RepairResultCard::from)
                .sorted(Comparator.comparing(RepairResultCard::repairId))
                .toList();
    }

    public synchronized RepairResultCard getRecord(String repairId) {
        return RepairResultCard.from(requireRecord(repairId));
    }

    public synchronized List<RepairRecord.ReviewEvent> history(String repairId) {
        return requireRecord(repairId).history();
    }

    public synchronized RepairResultCard submitReview(
            String repairId, RepairReviewRequest request) {
        RepairRecord record = requireRecord(repairId);
        record.submitReview(request);
        writeWorkflowReport();
        return RepairResultCard.from(record);
    }

    public synchronized BatchSnapshot getBatch(String batchId) {
        BatchSnapshot snapshot = batches.get(batchId);
        if (snapshot == null) {
            throw new RepairNotFoundException(batchId);
        }
        return snapshot;
    }

    public synchronized WorkflowSummary summary(String batchId) {
        BatchSnapshot batch = getBatch(batchId);
        List<RepairRecord> batchRecords = batch.repairIds().stream()
                .map(records::get)
                .toList();
        return summarize(batchId, batchRecords);
    }

    private WorkflowSummary summarize(String batchId, List<RepairRecord> values) {
        Map<String, Long> decisions = new LinkedHashMap<>();
        Map<String, Long> workflowStates = new LinkedHashMap<>();
        for (RepairRecord record : values) {
            decisions.merge(record.repairDecision().name(), 1L, Long::sum);
            workflowStates.merge(record.workflowState().name(), 1L, Long::sum);
        }
        long completed = values.stream()
                .filter(record -> "COMPLETED".equals(record.reviewStatus()))
                .count();
        long pending = values.stream()
                .filter(record -> "PENDING".equals(record.reviewStatus()))
                .count();
        return new WorkflowSummary(
                batchId,
                values.size(),
                decisions,
                workflowStates,
                6,
                completed,
                pending,
                values.stream().filter(row -> row.repairDecision() == RepairDecision.FINAL_SELECTED).count(),
                values.stream().filter(row -> row.repairDecision() == RepairDecision.RUNNER_UP).count(),
                "LOCAL_PROCESS_IDEMPOTENCY_ONLY");
    }

    private RepairRecord requireRecord(String repairId) {
        return Optional.ofNullable(records.get(repairId))
                .orElseThrow(() -> new RepairNotFoundException(repairId));
    }

    private void writeEvidence(
            RepairImportService.PreparedBatch prepared, List<RepairRecord> imported) {
        Path handoffTarget = javaRoot.resolve(
                "artifacts/week19/mainbase_repair_handoff_20260715.json");
        Path sourceHandoff = javaRoot.resolve(
                "../audio_engineering_repo_skeleton_v1/artifacts/manifests/repair_handoff_20260715.json")
                .normalize();
        try {
            Files.createDirectories(handoffTarget.getParent());
            Files.copy(sourceHandoff, handoffTarget, StandardCopyOption.REPLACE_EXISTING);
            writeJson(
                    javaRoot.resolve("artifacts/manifests/repair_artifact_index_20260716.json"),
                    Map.of(
                            "schemaVersion", "repair.artifact-index.v1",
                            "sourceCommit", prepared.sourceCommit(),
                            "importBatchId", prepared.batchId(),
                            "artifactCount", prepared.artifacts().size(),
                            "artifacts", prepared.artifacts()));
            writeRecordsCsv(imported);
            writeReviewQueue(imported);
            writeWorkflowReport();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write workflow evidence", exception);
        }
    }

    private void writeWorkflowReport() {
        if (batches.isEmpty()) {
            return;
        }
        BatchSnapshot latest = new ArrayList<>(batches.values()).get(batches.size() - 1);
        WorkflowSummary summary = summary(latest.batchId());
        writeJson(
                javaRoot.resolve("artifacts/manifests/repair_workflow_report_20260716.json"),
                Map.of(
                        "schemaVersion", "repair.workflow-report.v1",
                        "sourceCommit", latest.sourceCommit(),
                        "summary", summary,
                        "records", listRecords(),
                        "productionWorkflowVerified", false,
                        "distributedExactlyOnceClaimed", false));
    }

    private void writeRecordsCsv(List<RepairRecord> imported) throws IOException {
        StringBuilder csv = new StringBuilder(
                "repair_id,parent_candidate_id,workflow_state,repair_decision,review_status,"
                        + "review_version,before_sha256,after_sha256,source_commit\n");
        for (RepairRecord record : imported) {
            csv.append(record.repairId()).append(',')
                    .append(record.parentCandidateId()).append(',')
                    .append(record.workflowState()).append(',')
                    .append(record.repairDecision()).append(',')
                    .append(record.reviewStatus()).append(',')
                    .append(record.reviewVersion()).append(',')
                    .append(record.beforeArtifact().sha256()).append(',')
                    .append(record.afterArtifact().sha256()).append(',')
                    .append(record.sourceCommit()).append('\n');
        }
        writeText(
                javaRoot.resolve("artifacts/manifests/repair_workflow_records_20260716.csv"),
                csv.toString());
    }

    private void writeReviewQueue(List<RepairRecord> imported) throws IOException {
        StringBuilder csv = new StringBuilder(
                "repair_id,pair_id,preference,reason,confidence,audible_artifact,"
                        + "forbidden_event_status,reviewed_by,reviewed_at,submitted\n");
        List<RepairRecord> pending = imported.stream()
                .filter(record -> record.repairDecision() == RepairDecision.MANUAL_REVIEW)
                .limit(6)
                .toList();
        for (int index = 0; index < pending.size(); index++) {
            csv.append(pending.get(index).repairId()).append(',')
                    .append("review_").append(String.format("%02d", index + 1))
                    .append(",,,,,,,,false\n");
        }
        writeText(
                javaRoot.resolve("artifacts/week19/repair_manual_reviews_20260716.csv"),
                csv.toString());
    }

    private void writeJson(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write JSON evidence: " + path, exception);
        }
    }

    private static void writeText(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value, StandardCharsets.UTF_8);
    }

    public record ImportResult(String batchId, int recordCount, boolean reused) {
    }

    public record BatchSnapshot(
            String batchId,
            String sourceCommit,
            List<String> repairIds,
            java.time.Instant importedAt) {
    }

    public record WorkflowSummary(
            String batchId,
            int recordCount,
            Map<String, Long> decisionCounts,
            Map<String, Long> workflowStateCounts,
            int manualReviewRequestedCount,
            long manualReviewCompletedCount,
            long manualReviewPendingCount,
            long finalSelectedCount,
            long runnerUpCount,
            String idempotencyScope) {
    }
}
