package com.ryan.media.week18.lifecycle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class W18TaskLifecycleService {

    public enum State {
        CREATED,
        QUEUED,
        RUNNING,
        SUCCEEDED,
        REPAIR_REQUIRED,
        REPAIR_APPLIED,
        RESULT_BOUND
    }

    public enum ArtifactKind {
        WINNER,
        REPAIR_PROBE
    }

    public record Task(
            String taskId,
            String caseId,
            ArtifactKind artifactKind,
            String artifactKey,
            State state,
            String boundArtifactPath,
            List<String> history,
            Instant createdAt,
            Instant updatedAt
    ) {
        public Task {
            history = List.copyOf(history);
        }
    }

    private final ObjectMapper objectMapper;
    private final JsonNode contract;
    private final String sourceContract;
    private final String sourceContractSha256;

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public W18TaskLifecycleService(
            ObjectMapper objectMapper,
            Path contractPath
    ) throws IOException {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");

        Path normalizedPath = Objects.requireNonNull(
                contractPath,
                "contractPath"
        ).toAbsolutePath().normalize();

        if (!Files.isRegularFile(normalizedPath)) {
            throw new IllegalArgumentException(
                    "Contract file does not exist: " + normalizedPath
            );
        }

        this.contract = objectMapper.readTree(normalizedPath.toFile());
        this.sourceContract = normalizedPath.toString();
        this.sourceContractSha256 = sha256(normalizedPath);

        String contractName = contract.path("contract_name").asText();
        if (!"week18_selector_repair_handoff".equals(contractName)) {
            throw new IllegalArgumentException(
                    "Unexpected contract_name: " + contractName
            );
        }
    }

    public synchronized Task create(
            String caseId,
            ArtifactKind artifactKind,
            String artifactKey
    ) {
        String normalizedCaseId = requireNonBlank(caseId, "caseId");
        ArtifactKind normalizedKind = Objects.requireNonNull(
                artifactKind,
                "artifactKind"
        );
        String normalizedArtifactKey = requireNonBlank(
                artifactKey,
                "artifactKey"
        );

        findArtifact(
                normalizedCaseId,
                normalizedKind,
                normalizedArtifactKey
        );

        String taskId = "w18-task-%03d".formatted(
                sequence.incrementAndGet()
        );
        Instant now = Instant.now();

        Task task = new Task(
                taskId,
                normalizedCaseId,
                normalizedKind,
                normalizedArtifactKey,
                State.CREATED,
                null,
                List.of(State.CREATED.name()),
                now,
                now
        );

        tasks.put(taskId, task);
        return task;
    }

    public Task get(String taskId) {
        Task task = tasks.get(requireNonBlank(taskId, "taskId"));

        if (task == null) {
            throw new IllegalArgumentException(
                    "Task not found: " + taskId
            );
        }

        return task;
    }

    public synchronized Task queue(String taskId) {
        return transition(
                taskId,
                List.of(State.CREATED),
                State.QUEUED,
                null
        );
    }

    public synchronized Task start(String taskId) {
        return transition(
                taskId,
                List.of(State.QUEUED),
                State.RUNNING,
                null
        );
    }

    public synchronized Task decide(String taskId) {
        Task current = get(taskId);

        if (current.state() != State.RUNNING) {
            throw illegalTransition(
                    current,
                    List.of(State.RUNNING),
                    null
            );
        }

        State nextState = switch (current.artifactKind()) {
            case WINNER -> State.SUCCEEDED;
            case REPAIR_PROBE -> State.REPAIR_REQUIRED;
        };

        return transition(
                taskId,
                List.of(State.RUNNING),
                nextState,
                null
        );
    }

    public synchronized Task applyRepair(String taskId) {
        Task current = get(taskId);

        if (current.artifactKind() != ArtifactKind.REPAIR_PROBE) {
            throw new IllegalStateException(
                    "Only REPAIR_PROBE tasks can apply repair: "
                            + current.taskId()
            );
        }

        return transition(
                taskId,
                List.of(State.REPAIR_REQUIRED),
                State.REPAIR_APPLIED,
                null
        );
    }

    public synchronized Task bindResult(String taskId) {
        Task current = get(taskId);

        String artifactPath = resolveArtifactPath(
                current.caseId(),
                current.artifactKind(),
                current.artifactKey()
        );

        return transition(
                taskId,
                List.of(State.SUCCEEDED, State.REPAIR_APPLIED),
                State.RESULT_BOUND,
                artifactPath
        );
    }

    public ObjectNode resultCard(String taskId) {
        Task task = get(taskId);
        JsonNode summary = contract.path("summary");

        ObjectNode result = objectMapper.createObjectNode();
        result.put("taskId", task.taskId());
        result.put("caseId", task.caseId());
        result.put("artifactKind", task.artifactKind().name());
        result.put("artifactKey", task.artifactKey());
        result.put("state", task.state().name());

        if (task.boundArtifactPath() != null) {
            result.put("boundArtifactPath", task.boundArtifactPath());
        }

        result.put(
                "contractName",
                contract.path("contract_name").asText()
        );
        result.put(
                "contractVersion",
                contract.path("contract_version").asText()
        );
        result.put("sourceMainbaseContract", sourceContract);
        result.put("sourceContractSha256", sourceContractSha256);

        result.put(
                "winnerCount",
                summary.path("winner_count").asInt()
        );
        result.put(
                "repairProbeCount",
                summary.path("repair_probe_count").asInt()
        );
        result.put(
                "missingAssetCount",
                summary.path("missing_asset_count").asInt()
        );

        result.put("productionSloVerified", false);
        result.put("liveServiceAvailabilityClaim", false);

        return result;
    }

    public ObjectNode lifecycleReport() {
        JsonNode summary = contract.path("summary");

        ObjectNode report = objectMapper.createObjectNode();
        report.put("taskCount", tasks.size());
        report.put(
                "repairRequiredCount",
                countHistory(State.REPAIR_REQUIRED)
        );
        report.put(
                "succeededCount",
                countHistory(State.SUCCEEDED)
        );
        report.put(
                "repairAppliedCount",
                countHistory(State.REPAIR_APPLIED)
        );
        report.put(
                "resultBoundCount",
                countHistory(State.RESULT_BOUND)
        );

        report.put(
                "winnerCount",
                summary.path("winner_count").asInt()
        );
        report.put(
                "repairProbeCount",
                summary.path("repair_probe_count").asInt()
        );
        report.put(
                "missingAssetCount",
                summary.path("missing_asset_count").asInt()
        );

        report.put("sourceMainbaseContract", sourceContract);
        report.put("sourceContractSha256", sourceContractSha256);
        report.put("productionSloVerified", false);
        report.put("liveServiceAvailabilityClaim", false);

        return report;
    }

    private Task transition(
            String taskId,
            List<State> expectedStates,
            State nextState,
            String boundArtifactPath
    ) {
        Task current = get(taskId);

        if (!expectedStates.contains(current.state())) {
            throw illegalTransition(
                    current,
                    expectedStates,
                    nextState
            );
        }

        List<String> history = new ArrayList<>(current.history());
        history.add(nextState.name());

        Task updated = new Task(
                current.taskId(),
                current.caseId(),
                current.artifactKind(),
                current.artifactKey(),
                nextState,
                boundArtifactPath != null
                        ? boundArtifactPath
                        : current.boundArtifactPath(),
                history,
                current.createdAt(),
                Instant.now()
        );

        tasks.put(taskId, updated);
        return updated;
    }

    private IllegalStateException illegalTransition(
            Task current,
            List<State> expectedStates,
            State requestedState
    ) {
        return new IllegalStateException(
                "Illegal transition for task "
                        + current.taskId()
                        + ": current="
                        + current.state()
                        + ", expected="
                        + expectedStates
                        + ", requested="
                        + requestedState
        );
    }

    private long countHistory(State state) {
        return tasks.values().stream()
                .filter(task -> task.history().contains(state.name()))
                .count();
    }

    private JsonNode findArtifact(
            String caseId,
            ArtifactKind artifactKind,
            String artifactKey
    ) {
        String arrayField;
        String keyField;

        if (artifactKind == ArtifactKind.WINNER) {
            arrayField = "winners";
            keyField = "candidate_key";
        } else {
            arrayField = "repaired_candidates";
            keyField = "probe_id";
        }

        JsonNode artifacts = contract.path(arrayField);
        if (!artifacts.isArray()) {
            throw new IllegalStateException(
                    "Contract field is not an array: " + arrayField
            );
        }

        for (JsonNode artifact : artifacts) {
            boolean caseMatches = caseId.equals(
                    artifact.path("case_id").asText()
            );
            boolean keyMatches = artifactKey.equals(
                    artifact.path(keyField).asText()
            );

            if (caseMatches && keyMatches) {
                return artifact;
            }
        }

        throw new IllegalArgumentException(
                "Artifact not found in Mainbase contract: caseId="
                        + caseId
                        + ", kind="
                        + artifactKind
                        + ", key="
                        + artifactKey
        );
    }

    private String resolveArtifactPath(
            String caseId,
            ArtifactKind artifactKind,
            String artifactKey
    ) {
        JsonNode artifact = findArtifact(
                caseId,
                artifactKind,
                artifactKey
        );

        String path = artifactKind == ArtifactKind.WINNER
                ? artifact.path("audio_path").asText()
                : artifact.path("after_audio").path("path").asText();

        return requireNonBlank(path, "artifactPath");
    }

    private static String requireNonBlank(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(Files.readAllBytes(path))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}