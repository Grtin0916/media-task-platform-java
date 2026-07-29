package com.ryan.media.ranking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RankerArtifactVerifier {
    private final ObjectMapper mapper;

    public RankerArtifactVerifier(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void verify(Path bundleRoot, RankerBundleManifest manifest) {
        requireHexDigest(manifest.bundleDigest(), "bundleDigest");
        if (manifest.artifacts() == null || manifest.artifacts().isEmpty()) {
            throw new RankerException("INVALID_BUNDLE", "manifest artifacts are required");
        }
        Map<String, String> declared = new HashMap<>();
        for (RankerArtifactRef ref : manifest.artifacts()) {
            Path artifact = resolve(bundleRoot, ref.relativePath());
            String actual = sha256(artifact);
            if (!actual.equals(ref.sha256()) || fileSize(artifact) != ref.sizeBytes()) {
                throw new RankerException(
                        "RANKER_ARTIFACT_DIGEST_MISMATCH",
                        "artifact digest or size mismatch: " + ref.relativePath());
            }
            declared.put(ref.relativePath(), ref.sha256());
        }
        verifyFeatureSchema(bundleRoot, manifest);
        verifyChecksumFile(bundleRoot, declared);
    }

    private void verifyFeatureSchema(Path root, RankerBundleManifest manifest) {
        try {
            JsonNode schema = mapper.readTree(resolve(root, "feature-schema.json").toFile());
            if (!manifest.featureSchemaVersion().equals(schema.path("schemaVersion").asText())) {
                throw new RankerException(
                        "FEATURE_SCHEMA_MISMATCH",
                        "manifest and feature schema versions differ");
            }
        } catch (IOException exception) {
            throw new RankerException("INVALID_BUNDLE", "feature schema is not valid JSON");
        }
    }

    private void verifyChecksumFile(Path root, Map<String, String> declared) {
        Path checksums = resolve(root, "checksums.sha256");
        Map<String, String> entries = new HashMap<>();
        try {
            for (String line : Files.readAllLines(checksums)) {
                if (line.isBlank()) continue;
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length != 2) {
                    throw new RankerException("INVALID_BUNDLE", "malformed checksums.sha256");
                }
                Path file = resolve(root, parts[1]);
                if (!sha256(file).equals(parts[0])) {
                    throw new RankerException(
                            "RANKER_ARTIFACT_DIGEST_MISMATCH",
                            "checksum mismatch: " + parts[1]);
                }
                entries.put(parts[1], parts[0]);
            }
        } catch (IOException exception) {
            throw new RankerException("INVALID_BUNDLE", "cannot read checksums.sha256");
        }
        if (!entries.keySet().containsAll(declared.keySet())
                || !entries.containsKey("manifest.json")) {
            throw new RankerException("INVALID_BUNDLE", "checksum coverage is incomplete");
        }
        Set<String> expectedMembers = declared.keySet().stream().collect(Collectors.toSet());
        expectedMembers.add("manifest.json");
        expectedMembers.add("checksums.sha256");
        try (var files = Files.list(root)) {
            Set<String> actual = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
            if (!actual.equals(expectedMembers)) {
                throw new RankerException(
                        "INVALID_BUNDLE", "bundle directory contains undeclared members");
            }
        } catch (IOException exception) {
            throw new RankerException("INVALID_BUNDLE", "cannot list bundle");
        }
    }

    Path resolve(Path root, String relative) {
        if (relative == null || relative.isBlank() || Path.of(relative).isAbsolute()) {
            throw new RankerException(
                    "RANKER_PATH_OUTSIDE_ALLOWED_ROOT", "relative artifact path is required");
        }
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root) || relative.contains("..")) {
            throw new RankerException(
                    "RANKER_PATH_OUTSIDE_ALLOWED_ROOT", "path traversal is forbidden: " + relative);
        }
        try {
            Path realRoot = root.toRealPath();
            Path real = resolved.toRealPath();
            if (!real.startsWith(realRoot)
                    || !Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(resolved)) {
                throw new RankerException(
                        "RANKER_PATH_OUTSIDE_ALLOWED_ROOT", "symlink or non-file is forbidden");
            }
            return real;
        } catch (IOException exception) {
            throw new RankerException("RANKER_ARTIFACT_MISSING", relative);
        }
    }

    static String sha256(Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("cannot hash artifact", exception);
        }
    }

    private static long fileSize(Path path) {
        try { return Files.size(path); }
        catch (IOException exception) {
            throw new RankerException("RANKER_ARTIFACT_MISSING", path.toString());
        }
    }

    private static void requireHexDigest(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new RankerException("INVALID_BUNDLE", field + " must be lowercase SHA-256");
        }
    }
}
