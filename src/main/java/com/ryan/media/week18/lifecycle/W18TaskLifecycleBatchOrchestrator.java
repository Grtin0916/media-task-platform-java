package com.ryan.media.week18.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class W18TaskLifecycleBatchOrchestrator {

    private final W18TaskLifecycleService lifecycleService;
    private final JsonNode contract;

    private ObjectNode cachedReport;

    public W18TaskLifecycleBatchOrchestrator(
            W18TaskLifecycleService lifecycleService,
            ObjectMapper objectMapper,
            @Value(
                    "${w18.lifecycle.contract-path:"
                            + "artifacts/week18/"
                            + "w18_selector_repair_handoff_20260708.json}"
            )
            String contractPath
    ) throws IOException {
        this.lifecycleService = lifecycleService;

        Path path = Path.of(contractPath)
                .toAbsolutePath()
                .normalize();

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Contract file does not exist: " + path
            );
        }

        this.contract = objectMapper.readTree(path.toFile());

        String contractName =
                contract.path("contract_name").asText();

        if (!"week18_selector_repair_handoff".equals(contractName)) {
            throw new IllegalArgumentException(
                    "Unexpected contract_name: " + contractName
            );
        }
    }

    public synchronized ObjectNode bootstrap() {
        if (cachedReport != null) {
            ObjectNode replay = cachedReport.deepCopy();
            replay.put("replayed", true);
            return replay;
        }

        int existingTaskCount = lifecycleService
                .lifecycleReport()
                .path("taskCount")
                .asInt();

        if (existingTaskCount != 0) {
            throw new IllegalStateException(
                    "Batch bootstrap requires an empty lifecycle registry"
            );
        }

        int winnerTaskCount = orchestrateWinners();
        int repairTaskCount = orchestrateRepairCandidates();

        ObjectNode report = lifecycleService.lifecycleReport();

        report.put("batchOrchestrated", true);
        report.put("replayed", false);
        report.put(
                "orchestratedWinnerTaskCount",
                winnerTaskCount
        );
        report.put(
                "orchestratedRepairTaskCount",
                repairTaskCount
        );
        report.put(
                "orchestratedTaskCount",
                winnerTaskCount + repairTaskCount
        );

        cachedReport = report.deepCopy();
        return report.deepCopy();
    }

    private int orchestrateWinners() {
        JsonNode winners = requireArray("winners");
        int count = 0;

        for (JsonNode winner : winners) {
            String caseId = requiredText(
                    winner,
                    "case_id",
                    "winner"
            );
            String candidateKey = requiredText(
                    winner,
                    "candidate_key",
                    "winner"
            );

            W18TaskLifecycleService.Task task =
                    lifecycleService.create(
                            caseId,
                            W18TaskLifecycleService
                                    .ArtifactKind
                                    .WINNER,
                            candidateKey
                    );

            lifecycleService.queue(task.taskId());
            lifecycleService.start(task.taskId());
            lifecycleService.decide(task.taskId());
            lifecycleService.bindResult(task.taskId());

            count++;
        }

        return count;
    }

    private int orchestrateRepairCandidates() {
        JsonNode repairs = requireArray("repaired_candidates");
        int count = 0;

        for (JsonNode repair : repairs) {
            String caseId = requiredText(
                    repair,
                    "case_id",
                    "repair candidate"
            );
            String probeId = requiredText(
                    repair,
                    "probe_id",
                    "repair candidate"
            );

            W18TaskLifecycleService.Task task =
                    lifecycleService.create(
                            caseId,
                            W18TaskLifecycleService
                                    .ArtifactKind
                                    .REPAIR_PROBE,
                            probeId
                    );

            lifecycleService.queue(task.taskId());
            lifecycleService.start(task.taskId());
            lifecycleService.decide(task.taskId());
            lifecycleService.applyRepair(task.taskId());
            lifecycleService.bindResult(task.taskId());

            count++;
        }

        return count;
    }

    private JsonNode requireArray(String fieldName) {
        JsonNode value = contract.path(fieldName);

        if (!value.isArray()) {
            throw new IllegalStateException(
                    "Contract field is not an array: " + fieldName
            );
        }

        return value;
    }

    private String requiredText(
            JsonNode node,
            String fieldName,
            String context
    ) {
        String value = node.path(fieldName).asText();

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing " + fieldName + " in " + context
            );
        }

        return value;
    }
}