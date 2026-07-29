package com.ryan.media.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RankerEvidenceWriter {
    private final ObjectMapper mapper;
    private final RankerRegistry registry;
    private final Path javaRoot;

    public RankerEvidenceWriter(
            ObjectMapper mapper,
            RankerRegistry registry,
            @Value("${ranker.java-root:.}") String javaRoot) {
        this.mapper = mapper;
        this.registry = registry;
        this.javaRoot = Path.of(javaRoot).toAbsolutePath().normalize();
    }

    public synchronized void record(RankerRegistry.ImportResult result) {
        try {
            Path events = javaRoot.resolve("artifacts/runtime/rankers/ranker-events.jsonl");
            Path report = javaRoot.resolve("artifacts/manifests/w21_ranker_version_report.json");
            Path csv = javaRoot.resolve("artifacts/manifests/w21_ranker_versions.csv");
            Files.createDirectories(events.getParent());
            Files.createDirectories(report.getParent());
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", result.reused() ? "RANKER_IMPORT_REUSED" : "RANKER_REGISTERED");
            event.put("rankerVersion", result.version().rankerVersion());
            event.put("bundleDigest", result.version().bundleDigest());
            event.put("promotionStatus", result.version().promotionStatus());
            event.put("recommendationCount", result.version().recommendationCount());
            event.put("finalSelectedMutationCount", result.version().finalSelectedMutationCount());
            event.put("occurredAt", Instant.now().toString());
            Files.writeString(
                    events,
                    mapper.writeValueAsString(event) + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("schemaVersion", "ranker-version-report/v1");
            body.put("versionCount", registry.list().size());
            body.put("versions", registry.list());
            body.put("finalSelectedMutationCount", registry.list().stream()
                    .mapToInt(RankerVersion::finalSelectedMutationCount).sum());
            mapper.writerWithDefaultPrettyPrinter().writeValue(report.toFile(), body);

            StringBuilder table = new StringBuilder(
                    "rankerVersion,bundleDigest,promotionStatus,modelPresent,oofAvailable,"
                            + "recommendationCount,reviewSubmittedCount,finalSelectedMutationCount\n");
            for (RankerVersion version : registry.list()) {
                table.append(version.rankerVersion()).append(',')
                        .append(version.bundleDigest()).append(',')
                        .append(version.promotionStatus()).append(',')
                        .append(version.modelPresent()).append(',')
                        .append(version.oofAvailable()).append(',')
                        .append(version.recommendationCount()).append(',')
                        .append(version.reviewSubmittedCount()).append(',')
                        .append(version.finalSelectedMutationCount()).append('\n');
            }
            Files.writeString(csv, table.toString());
        } catch (IOException exception) {
            throw new RankerException("RANKER_EVIDENCE_WRITE_FAILED", exception.getMessage());
        }
    }
}
