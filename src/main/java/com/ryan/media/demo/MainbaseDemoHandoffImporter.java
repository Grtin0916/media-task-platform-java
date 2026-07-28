package com.ryan.media.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MainbaseDemoHandoffImporter {
    private final ObjectMapper mapper;
    private final Path mainbaseRoot;
    private final Path javaRoot;
    private final String handoffPath;

    public MainbaseDemoHandoffImporter(
            ObjectMapper mapper,
            @Value("${demo.runner.mainbase-root:../audio_engineering_repo_skeleton_v1}") String mainbaseRoot,
            @Value("${demo.runner.java-root:.}") String javaRoot,
            @Value("${demo.runner.handoff:artifacts/manifests/dss_rerank_repair_handoff_20260722.json}") String handoffPath) {
        this.mapper = mapper;
        this.mainbaseRoot = Path.of(mainbaseRoot).toAbsolutePath().normalize();
        this.javaRoot = Path.of(javaRoot).toAbsolutePath().normalize();
        this.handoffPath = handoffPath;
    }

    public MainbaseDemoHandoff importHandoff() {
        Path handoff = resolve(handoffPath);
        byte[] bytes = read(handoff);
        try {
            Path frozen = javaRoot.resolve("artifacts/week20/mainbase_dss_rerank_repair_handoff_20260722.json");
            Files.createDirectories(frozen.getParent());
            Files.write(frozen, bytes);
        } catch (IOException e) { throw new DemoJobException("ARTIFACT_COPY_FAILED", e.getMessage()); }
        JsonNode root;
        try { root = mapper.readTree(bytes); }
        catch (IOException e) { throw new DemoJobException("INVALID_HANDOFF", e.getMessage()); }
        if (root.path("finalSelectedCount").asInt(-1) != 0) {
            throw new DemoJobException("FINAL_SELECTION_DRIFT", "handoff finalSelectedCount must remain zero");
        }
        String sourceCommit = required(root, "sourceCommit");
        List<DemoResultSeed> seeds = new ArrayList<>();
        for (JsonNode node : root.path("records")) {
            DemoPublishDecision publish = parsePublish(node);
            DemoArtifactRef selected = materializeOptional(node, sourceCommit, "selectedArtifact", "selectedDigest");
            DemoArtifactRef repair = materializeOptional(node, sourceCommit, "repairArtifact", "repairDigest");
            boolean finalSelected = node.path("claimBoundary").path("final_selected").asBoolean(true);
            boolean preference = node.path("claimBoundary").path("human_preference_proven").asBoolean(true);
            if (finalSelected || preference || publish == DemoPublishDecision.FINAL_SELECTED) {
                throw new DemoJobException("CLAIM_BOUNDARY_DRIFT", required(node, "caseId"));
            }
            seeds.add(new DemoResultSeed(required(node, "caseId"), required(node, "sourceCaseId"),
                    required(node, "repairDecision"), publish, selected, repair,
                    required(node, "repairAction"), node.path("editApplied").asBoolean(),
                    node.path("liveGroupAvailable").asBoolean(), false, false));
        }
        long provisional = seeds.stream().filter(x -> x.publishDecision() == DemoPublishDecision.PROVISIONAL_SELECTED).count();
        long blocked = seeds.stream().filter(x -> x.publishDecision() != DemoPublishDecision.PROVISIONAL_SELECTED).count();
        if (seeds.size() != 12 || provisional != 10 || blocked != 2) {
            throw new DemoJobException("HANDOFF_COUNT_DRIFT", "expected 12/10/2 records");
        }
        return new MainbaseDemoHandoff(root.path("schemaVersion").asText(), sourceCommit,
                sha256(bytes), List.copyOf(seeds), (int) provisional, (int) blocked, 0);
    }

    private DemoPublishDecision parsePublish(JsonNode node) {
        String value = required(node, "publishDecision");
        if ("BLOCKED".equals(value)) {
            return "REPAIR_REJECTED".equals(node.path("repairDecision").asText())
                    ? DemoPublishDecision.REPAIR_REJECTED : DemoPublishDecision.REPAIR_BLOCKED;
        }
        try { return DemoPublishDecision.valueOf(value); }
        catch (IllegalArgumentException e) { throw new DemoJobException("UNKNOWN_PUBLISH_DECISION", value); }
    }

    private DemoArtifactRef materializeOptional(JsonNode node, String commit, String pathField, String digestField) {
        String relative = node.path(pathField).asText("");
        if (relative.isBlank()) return null;
        Path source = resolve(relative);
        String actual = "sha256:" + sha256(source);
        String expected = required(node, digestField);
        if (!actual.equals(expected)) {
            throw new DemoJobException("HANDOFF_DIGEST_MISMATCH", relative);
        }
        Path target = javaRoot.resolve("artifacts/week20/blobs/sha256")
                .resolve(actual.substring(7)).normalize();
        if (!target.startsWith(javaRoot)) throw new DemoJobException("PATH_OUTSIDE_JAVA", relative);
        try {
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            if (!actual.equals("sha256:" + sha256(target))) {
                throw new DemoJobException("RESULT_DIGEST_MISMATCH", relative);
            }
            return new DemoArtifactRef(commit, relative, actual,
                    javaRoot.relativize(target).toString().replace('\\', '/'),
                    Files.size(target), "audio/wav", "SHA256_VERIFIED");
        } catch (IOException e) { throw new DemoJobException("ARTIFACT_COPY_FAILED", e.getMessage()); }
    }

    private Path resolve(String relative) {
        if (relative == null || relative.isBlank() || Path.of(relative).isAbsolute()) {
            throw new DemoJobException("PATH_OUTSIDE_MAINBASE", String.valueOf(relative));
        }
        Path candidate = mainbaseRoot.resolve(relative).normalize();
        if (!candidate.startsWith(mainbaseRoot)) throw new DemoJobException("PATH_OUTSIDE_MAINBASE", relative);
        try {
            Path realRoot = mainbaseRoot.toRealPath();
            Path real = candidate.toRealPath();
            if (!real.startsWith(realRoot) || !Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                throw new DemoJobException("PATH_OUTSIDE_MAINBASE", relative);
            }
            return real;
        } catch (IOException e) { throw new DemoJobException("ARTIFACT_MISSING", relative); }
    }
    private byte[] read(Path p) {
        try { return Files.readAllBytes(p); }
        catch (IOException e) { throw new DemoJobException("ARTIFACT_MISSING", p.toString()); }
    }
    private static String required(JsonNode n, String field) {
        String value = n.path(field).asText("");
        if (value.isBlank()) throw new DemoJobException("INVALID_HANDOFF", "missing " + field);
        return value;
    }
    static String sha256(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) >= 0) md.update(b, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
