package com.ryan.media.repair;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RepairImportService {
    static final String SOURCE_REPOSITORY =
            "github.com/Grtin0916/A-structured-sound-layer-generation-system-for-generative-video-and-long-video-creation-workflows";

    private final ObjectMapper objectMapper;
    private final Path mainbaseRoot;
    private final Path javaRoot;
    private final String sourceCommit;

    public RepairImportService(
            ObjectMapper objectMapper,
            @Value("${repair.mainbase-root:../audio_engineering_repo_skeleton_v1}") String mainbaseRoot,
            @Value("${repair.java-root:.}") String javaRoot,
            @Value("${repair.source-commit:b1e19c1}") String sourceCommit) {
        this.objectMapper = objectMapper;
        this.mainbaseRoot = Path.of(mainbaseRoot).toAbsolutePath().normalize();
        this.javaRoot = Path.of(javaRoot).toAbsolutePath().normalize();
        this.sourceCommit = sourceCommit;
    }

    public PreparedBatch prepare(String handoffRelativePath) {
        Path handoff = resolveContained(handoffRelativePath, "handoff");
        byte[] handoffBytes = readAll(handoff, handoffRelativePath);
        JsonNode root;
        try {
            root = objectMapper.readTree(handoffBytes);
        } catch (IOException exception) {
            throw new ArtifactImportException("invalid handoff JSON", handoffRelativePath);
        }
        JsonNode inputRecords = root.path("records");
        if (!inputRecords.isArray()) {
            throw new ArtifactImportException("handoff records must be an array", handoffRelativePath);
        }
        String batchId = sha256(concat(
                handoffBytes,
                SOURCE_REPOSITORY.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                sourceCommit.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        List<PreparedRecord> records = new ArrayList<>();
        List<ArtifactRef> artifacts = new ArrayList<>();
        for (JsonNode node : inputRecords) {
            String repairId = requiredText(node, "repair_id");
            ArtifactRef before = materialize(requiredText(node, "before_artifact"));
            ArtifactRef after = materialize(requiredText(node, "after_artifact"));
            artifacts.add(before);
            artifacts.add(after);
            RepairDecision decision;
            try {
                decision = RepairDecision.valueOf(requiredText(node, "decision"));
            } catch (IllegalArgumentException exception) {
                throw new ArtifactImportException(
                        "unknown upstream repair decision", repairId);
            }
            records.add(new PreparedRecord(
                    repairId,
                    requiredText(node, "parent_candidate_id"),
                    failureId(repairId),
                    requiredText(node, "repair_action"),
                    requiredText(node, "source_mode"),
                    node.path("metrics"),
                    node.path("reason").asText(""),
                    before,
                    after,
                    decision));
        }
        if (records.size() != 20) {
            throw new ArtifactImportException(
                    "expected exactly 20 handoff records, found " + records.size(),
                    handoffRelativePath);
        }
        return new PreparedBatch(batchId, sourceCommit, records, artifacts, Instant.now());
    }

    private ArtifactRef materialize(String relativePath) {
        Path source = resolveContained(relativePath, "artifact");
        String digest = sha256(source);
        Path blob = javaRoot.resolve("artifacts/week19/blobs/sha256")
                .resolve(digest + extension(source))
                .normalize();
        if (!blob.startsWith(javaRoot)) {
            throw new ArtifactImportException("materialized path escaped Java root", relativePath);
        }
        try {
            Files.createDirectories(blob.getParent());
            if (!Files.exists(blob)) {
                Files.copy(source, blob, StandardCopyOption.COPY_ATTRIBUTES);
            }
            if (!digest.equals(sha256(blob))) {
                throw new ArtifactImportException(
                        "materialized artifact digest mismatch", relativePath);
            }
            return new ArtifactRef(
                    SOURCE_REPOSITORY,
                    sourceCommit,
                    relativePath,
                    source.toString(),
                    javaRoot.relativize(blob).toString().replace('\\', '/'),
                    digest,
                    Files.size(source),
                    "audio/wav",
                    true,
                    "SHA256_VERIFIED");
        } catch (IOException exception) {
            throw new ArtifactImportException(
                    "failed to materialize artifact: " + exception.getMessage(), relativePath);
        }
    }

    private Path resolveContained(String relativePath, String kind) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ArtifactImportException(kind + " path is required", relativePath);
        }
        Path supplied = Path.of(relativePath);
        if (supplied.isAbsolute()) {
            throw new ArtifactImportException("absolute path is forbidden", relativePath);
        }
        Path resolved = mainbaseRoot.resolve(supplied).normalize();
        if (!resolved.startsWith(mainbaseRoot)) {
            throw new ArtifactImportException("path traversal is forbidden", relativePath);
        }
        try {
            Path realRoot = mainbaseRoot.toRealPath();
            Path real = resolved.toRealPath();
            if (!real.startsWith(realRoot)
                    || !Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                throw new ArtifactImportException(
                        "symlink escape or non-regular file is forbidden", relativePath);
            }
            return real;
        } catch (IOException exception) {
            throw new ArtifactImportException("artifact is missing", relativePath);
        }
    }

    private byte[] readAll(Path path, String reportedPath) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new ArtifactImportException("cannot read artifact", reportedPath);
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("");
        if (value.isBlank()) {
            throw new ArtifactImportException("required field is missing: " + field, field);
        }
        return value;
    }

    private static String failureId(String repairId) {
        int suffix = repairId.indexOf('_', 3);
        return suffix > 0 ? repairId.substring(0, suffix) : repairId;
    }

    private static String extension(Path source) {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".bin";
    }

    private static byte[] concat(byte[]... values) {
        int size = 0;
        for (byte[] value : values) {
            size += value.length;
        }
        byte[] result = new byte[size];
        int offset = 0;
        for (byte[] value : values) {
            System.arraycopy(value, 0, result, offset, value.length);
            offset += value.length;
        }
        return result;
    }

    private static String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record PreparedRecord(
            String repairId,
            String parentCandidateId,
            String failureId,
            String repairAction,
            String sourceMode,
            JsonNode metrics,
            String reason,
            ArtifactRef beforeArtifact,
            ArtifactRef afterArtifact,
            RepairDecision repairDecision) {
    }

    public record PreparedBatch(
            String batchId,
            String sourceCommit,
            List<PreparedRecord> records,
            List<ArtifactRef> artifacts,
            Instant importedAt) {
    }
}
