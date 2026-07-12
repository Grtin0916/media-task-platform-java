package com.ryan.media.week18.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = W18TaskLifecycleTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "w18.lifecycle.contract-path="
                        + "artifacts/week18/"
                        + "w18_selector_repair_handoff_20260708.json",
                "w18.lifecycle.test-context=batch"
        }
)
class W18TaskLifecycleBatchHttpIT {

    private static final String BOOTSTRAP_PATH =
            "/api/week18/lifecycle/bootstrap";

    private static final Path REPORT_PATH = Path.of(
            "artifacts/manifests/"
                    + "w18_task_lifecycle_report_20260712.json"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void orchestratesEntireMainbaseContractIdempotently()
            throws Exception {
        JsonNode first = postBootstrap();

        assertFalse(first.path("replayed").asBoolean());
        assertTrue(first.path("batchOrchestrated").asBoolean());

        assertEquals(
                6,
                first.path("orchestratedWinnerTaskCount").asInt()
        );
        assertEquals(
                6,
                first.path("orchestratedRepairTaskCount").asInt()
        );
        assertEquals(
                12,
                first.path("orchestratedTaskCount").asInt()
        );

        assertEquals(
                12,
                first.path("taskCount").asInt()
        );
        assertEquals(
                6,
                first.path("succeededCount").asInt()
        );
        assertEquals(
                6,
                first.path("repairRequiredCount").asInt()
        );
        assertEquals(
                6,
                first.path("repairAppliedCount").asInt()
        );
        assertEquals(
                12,
                first.path("resultBoundCount").asInt()
        );

        assertEquals(
                6,
                first.path("winnerCount").asInt()
        );
        assertEquals(
                6,
                first.path("repairProbeCount").asInt()
        );
        assertEquals(
                0,
                first.path("missingAssetCount").asInt()
        );

        assertEquals(
                64,
                first.path("sourceContractSha256")
                        .asText()
                        .length()
        );

        Files.createDirectories(REPORT_PATH.getParent());

        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(REPORT_PATH.toFile(), first);

        assertTrue(Files.isRegularFile(REPORT_PATH));
        assertTrue(Files.size(REPORT_PATH) > 0);

        JsonNode second = postBootstrap();

        assertTrue(second.path("replayed").asBoolean());
        assertEquals(
                12,
                second.path("taskCount").asInt()
        );
        assertEquals(
                12,
                second.path("resultBoundCount").asInt()
        );
    }

    private JsonNode postBootstrap() {
        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity(
                        BOOTSTRAP_PATH,
                        Map.of(),
                        JsonNode.class
                );

        assertEquals(
                HttpStatus.OK.value(),
                response.getStatusCode().value()
        );

        JsonNode body = response.getBody();
        assertNotNull(body);

        return body;
    }
}