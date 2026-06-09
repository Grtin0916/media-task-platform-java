package com.ryan.media.week13;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week13AudioArtifactRegistrySnapshotExportTest {

    private static final List<String> REQUIRED_FIELDS = List.of(
            "candidateId",
            "audioUri",
            "sourceType",
            "assetTimeMode",
            "expectedStartSec",
            "globalStartSec",
            "placementRequired",
            "status"
    );

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exportCandidateLevelSnapshotFromRuntimeRegistry() throws Exception {
        Extraction best = extractBestRuntimeCandidateList();

        List<Map<String, Object>> artifacts = normalizeAndDedupe(best.items());

        List<Map<String, Object>> missingFields = new ArrayList<>();
        for (Map<String, Object> item : artifacts) {
            Object candidateId = item.get("candidateId");
            for (String field : REQUIRED_FIELDS) {
                if (!item.containsKey(field) || item.get(field) == null || "".equals(item.get(field))) {
                    Map<String, Object> miss = new LinkedHashMap<>();
                    miss.put("candidateId", candidateId);
                    miss.put("missing", field);
                    missingFields.add(miss);
                }
            }
        }

        List<String> duplicateCandidateIds = findDuplicateCandidateIds(artifacts);

        List<String> blockers = new ArrayList<>();
        if (artifacts.isEmpty()) {
            blockers.add("NO_CANDIDATE_LEVEL_RUNTIME_REGISTRY_DATA_FOUND");
        }
        if (artifacts.size() != 10) {
            blockers.add("CANDIDATE_COUNT_NOT_10:" + artifacts.size());
        }
        if (!missingFields.isEmpty()) {
            blockers.add("REQUIRED_FIELDS_MISSING");
        }
        if (!duplicateCandidateIds.isEmpty()) {
            blockers.add("DUPLICATE_CANDIDATE_ID");
        }

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", blockers.isEmpty() ? "PASS" : "FAIL");
        snapshot.put("scope", "week13_java_audio_artifact_registry_snapshot_v1_runtime");
        snapshot.put("generatedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        snapshot.put("source", best.source());
        snapshot.put("candidateCount", artifacts.size());
        snapshot.put("requiredFields", REQUIRED_FIELDS);
        snapshot.put("missingFields", missingFields);
        snapshot.put("duplicateCandidateIds", duplicateCandidateIds);
        snapshot.put("artifacts", artifacts);
        snapshot.put("runtimeExtractionCandidates", best.trace());
        snapshot.put("blockers", blockers);
        snapshot.put(
                "boundaryStatement",
                "Snapshot is exported from Java runtime registry for metadata consistency only; "
                        + "it is not durable registry, object storage, final mixer, semantic quality, "
                        + "human audition, or production readiness."
        );

        Path out = Path.of("artifacts/manifests/week13_java_audio_artifact_registry_snapshot.json");
        Files.createDirectories(out.getParent());
        Files.writeString(
                out,
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot),
                StandardCharsets.UTF_8
        );

        if (!blockers.isEmpty()) {
            fail("Runtime registry snapshot export failed: "
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot));
        }
    }

    private Extraction extractBestRuntimeCandidateList() {
        List<Object> beans = new ArrayList<>();
        List<String> trace = new ArrayList<>();

        tryAddBean(beans, trace, AudioArtifactRegistryService.class);
        tryAddBean(beans, trace, AudioArtifactRegistryController.class);

        Extraction best = new Extraction("NONE", List.of(), trace);

        for (Object bean : beans) {
            Class<?> type = bean.getClass();
            for (Method method : type.getMethods()) {
                if (!isCandidateExportMethod(method)) {
                    continue;
                }

                String source = type.getName() + "#" + method.getName();
                try {
                    Object value = method.invoke(bean);
                    List<Map<String, Object>> found = new ArrayList<>();
                    collectCandidateLikeMaps(value, found, new IdentityHashMap<>());
                    trace.add(source + " -> candidateLikeCount=" + found.size());

                    if (found.size() > best.items().size()) {
                        best = new Extraction(source, found, trace);
                    }
                    if (found.size() == 10) {
                        return best;
                    }
                } catch (Exception e) {
                    trace.add(source + " -> ERROR:" + e.getClass().getSimpleName() + ":" + e.getMessage());
                }
            }
        }

        return best;
    }

    private void tryAddBean(List<Object> beans, List<String> trace, Class<?> beanType) {
        try {
            Object bean = applicationContext.getBean(beanType);
            beans.add(bean);
            trace.add("bean=" + beanType.getName() + " FOUND");
        } catch (NoSuchBeanDefinitionException e) {
            trace.add("bean=" + beanType.getName() + " MISSING");
        }
    }

    private boolean isCandidateExportMethod(Method method) {
        if (method.getParameterCount() != 0) {
            return false;
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        if (method.getDeclaringClass().equals(Object.class)) {
            return false;
        }
        if (Void.TYPE.equals(method.getReturnType())) {
            return false;
        }

        String name = method.getName().toLowerCase();
        return name.contains("artifact")
                || name.contains("registry")
                || name.contains("candidate")
                || name.contains("list")
                || name.contains("all");
    }

    @SuppressWarnings("unchecked")
    private void collectCandidateLikeMaps(
            Object value,
            List<Map<String, Object>> out,
            IdentityHashMap<Object, Boolean> seen
    ) {
        if (value == null) {
            return;
        }

        if (value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value.getClass().isEnum()) {
            return;
        }

        if (seen.containsKey(value)) {
            return;
        }
        seen.put(value, Boolean.TRUE);

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectCandidateLikeMaps(item, out, seen);
            }
            return;
        }

        if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            for (Object item : arr) {
                collectCandidateLikeMaps(item, out, seen);
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                converted.put(String.valueOf(e.getKey()), e.getValue());
            }

            if (looksCandidateLike(converted)) {
                out.add(converted);
                return;
            }

            for (Object nested : converted.values()) {
                collectCandidateLikeMaps(nested, out, seen);
            }
            return;
        }

        try {
            Map<String, Object> converted = objectMapper.convertValue(
                    value,
                    new TypeReference<Map<String, Object>>() {}
            );

            if (looksCandidateLike(converted)) {
                out.add(converted);
                return;
            }

            for (Object nested : converted.values()) {
                collectCandidateLikeMaps(nested, out, seen);
            }
        } catch (IllegalArgumentException ignored) {
            // Non-serializable runtime helper object; ignore.
        }
    }

    private boolean looksCandidateLike(Map<String, Object> map) {
        return hasAny(map, "candidateId", "candidate_id", "audioCandidateId")
                || hasAny(map, "audioUri", "audio_uri", "sourceAudioUri")
                || hasAny(map, "assetTimeMode", "timeMode", "asset_time_mode");
    }

    private boolean hasAny(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> normalizeAndDedupe(List<Map<String, Object>> rawItems) {
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();

        for (Map<String, Object> raw : rawItems) {
            Map<String, Object> normalized = normalizeItem(raw);
            Object candidateId = normalized.get("candidateId");
            if (candidateId == null || "".equals(candidateId)) {
                candidateId = "__missing_candidate_id_" + byId.size();
            }
            byId.putIfAbsent(String.valueOf(candidateId), normalized);
        }

        return new ArrayList<>(byId.values());
    }

    private Map<String, Object> normalizeItem(Map<String, Object> raw) {
        Map<String, Object> out = new LinkedHashMap<>();

        copyCanonical(out, raw, "candidateId", "candidateId", "candidate_id", "audioCandidateId", "id");
        copyCanonical(out, raw, "audioUri", "audioUri", "audio_uri", "sourceAudioUri", "localAudioUri", "path");
        copyCanonical(out, raw, "sourceType", "sourceType", "source_type", "generatorName", "source");
        copyCanonical(out, raw, "assetTimeMode", "assetTimeMode", "timeMode", "asset_time_mode");
        copyCanonical(out, raw, "expectedStartSec", "expectedStartSec", "expected_start_sec", "startSec");
        copyCanonical(out, raw, "globalStartSec", "globalStartSec", "global_start_sec", "placementStartSec");
        copyCanonical(out, raw, "placementRequired", "placementRequired", "placement_required");
        copyCanonical(out, raw, "status", "status", "candidateStatus");

        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            out.putIfAbsent(entry.getKey(), entry.getValue());
        }

        return out;
    }

    private void copyCanonical(
            Map<String, Object> out,
            Map<String, Object> raw,
            String canonical,
            String... aliases
    ) {
        for (String alias : aliases) {
            if (raw.containsKey(alias)) {
                out.put(canonical, raw.get(alias));
                return;
            }
        }
    }

    private List<String> findDuplicateCandidateIds(List<Map<String, Object>> artifacts) {
        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicated = new LinkedHashSet<>();

        for (Map<String, Object> item : artifacts) {
            Object candidateId = item.get("candidateId");
            if (candidateId == null) {
                continue;
            }
            String key = String.valueOf(candidateId);
            if (!seen.add(key)) {
                duplicated.add(key);
            }
        }

        return new ArrayList<>(duplicated);
    }

    private record Extraction(
            String source,
            List<Map<String, Object>> items,
            List<String> trace
    ) {
    }
}