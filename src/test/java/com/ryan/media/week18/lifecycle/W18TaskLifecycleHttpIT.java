package com.ryan.media.week18.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
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
                        + "w18_selector_repair_handoff_20260708.json"
        }
)
class W18TaskLifecycleHttpIT {

    private static final String BASE =
            "/api/week18/lifecycle";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesArtifactDrivenLifecycleOverRealHttp() {
        JsonNode winnerCreated = post(
                BASE + "/tasks",
                Map.of(
                        "caseId",
                        "glass_drop_room_001",
                        "artifactKind",
                        "WINNER",
                        "artifactKey",
                        "glass_drop_room_001|dss_layer_avoid"
                ),
                HttpStatus.CREATED
        );

        String winnerTaskId =
                winnerCreated.path("taskId").asText();

        assertTrue(!winnerTaskId.isBlank());
        assertEquals(
                "CREATED",
                winnerCreated.path("state").asText()
        );

        JsonNode queriedWinner = get(
                BASE + "/tasks/" + winnerTaskId,
                HttpStatus.OK
        );

        assertEquals(
                winnerTaskId,
                queriedWinner.path("taskId").asText()
        );

        assertEquals(
                "QUEUED",
                postAction(
                        BASE + "/tasks/"
                                + winnerTaskId
                                + "/queue",
                        HttpStatus.OK
                ).path("state").asText()
        );

        assertEquals(
                "RUNNING",
                postAction(
                        BASE + "/tasks/"
                                + winnerTaskId
                                + "/start",
                        HttpStatus.OK
                ).path("state").asText()
        );

        assertEquals(
                "SUCCEEDED",
                postAction(
                        BASE + "/tasks/"
                                + winnerTaskId
                                + "/decide",
                        HttpStatus.OK
                ).path("state").asText()
        );

        JsonNode winnerBound = postAction(
                BASE + "/tasks/"
                        + winnerTaskId
                        + "/bind-result",
                HttpStatus.OK
        );

        assertEquals(
                "RESULT_BOUND",
                winnerBound.path("state").asText()
        );
        assertTrue(
                winnerBound.path("boundArtifactPath")
                        .asText()
                        .contains("w18_010_glass_drop_room_001")
        );

        JsonNode winnerCard = get(
                BASE + "/tasks/"
                        + winnerTaskId
                        + "/result-card",
                HttpStatus.OK
        );

        assertEquals(
                6,
                winnerCard.path("winnerCount").asInt()
        );
        assertEquals(
                6,
                winnerCard.path("repairProbeCount").asInt()
        );
        assertEquals(
                0,
                winnerCard.path("missingAssetCount").asInt()
        );
        assertEquals(
                64,
                winnerCard.path("sourceContractSha256")
                        .asText()
                        .length()
        );

        JsonNode repairCreated = post(
                BASE + "/tasks",
                Map.of(
                        "caseId",
                        "glass_drop_room_001",
                        "artifactKind",
                        "REPAIR_PROBE",
                        "artifactKey",
                        "mr_001"
                ),
                HttpStatus.CREATED
        );

        String repairTaskId =
                repairCreated.path("taskId").asText();

        postAction(
                BASE + "/tasks/"
                        + repairTaskId
                        + "/queue",
                HttpStatus.OK
        );
        postAction(
                BASE + "/tasks/"
                        + repairTaskId
                        + "/start",
                HttpStatus.OK
        );

        assertEquals(
                "REPAIR_REQUIRED",
                postAction(
                        BASE + "/tasks/"
                                + repairTaskId
                                + "/decide",
                        HttpStatus.OK
                ).path("state").asText()
        );

        assertEquals(
                "REPAIR_APPLIED",
                postAction(
                        BASE + "/tasks/"
                                + repairTaskId
                                + "/repair",
                        HttpStatus.OK
                ).path("state").asText()
        );

        JsonNode repairBound = postAction(
                BASE + "/tasks/"
                        + repairTaskId
                        + "/bind-result",
                HttpStatus.OK
        );

        assertEquals(
                "RESULT_BOUND",
                repairBound.path("state").asText()
        );
        assertTrue(
                repairBound.path("boundArtifactPath")
                        .asText()
                        .contains(
                                "silence_trim_or_gain__after.wav"
                        )
        );

        JsonNode report = get(
                BASE + "/report",
                HttpStatus.OK
        );

        assertEquals(
                2,
                report.path("taskCount").asInt()
        );
        assertEquals(
                1,
                report.path("succeededCount").asInt()
        );
        assertEquals(
                1,
                report.path("repairRequiredCount").asInt()
        );
        assertEquals(
                1,
                report.path("repairAppliedCount").asInt()
        );
        assertEquals(
                2,
                report.path("resultBoundCount").asInt()
        );

        JsonNode illegalCreated = post(
                BASE + "/tasks",
                Map.of(
                        "caseId",
                        "street_rain_crosswalk_001",
                        "artifactKind",
                        "WINNER",
                        "artifactKey",
                        "street_rain_crosswalk_001"
                                + "|dss_event_timeline"
                ),
                HttpStatus.CREATED
        );

        String illegalTaskId =
                illegalCreated.path("taskId").asText();

        JsonNode conflictProblem = postAction(
                BASE + "/tasks/"
                        + illegalTaskId
                        + "/start",
                HttpStatus.CONFLICT
        );

        assertEquals(
                409,
                conflictProblem.path("status").asInt()
        );
        assertEquals(
                "W18_INVALID_STATE_TRANSITION",
                conflictProblem.path("code").asText()
        );

        JsonNode unknownArtifactProblem = post(
                BASE + "/tasks",
                Map.of(
                        "caseId",
                        "unknown_case",
                        "artifactKind",
                        "WINNER",
                        "artifactKey",
                        "missing_candidate"
                ),
                HttpStatus.BAD_REQUEST
        );

        assertEquals(
                "W18_BAD_REQUEST",
                unknownArtifactProblem.path("code").asText()
        );

        JsonNode missingTaskProblem = get(
                BASE + "/tasks/w18-task-does-not-exist",
                HttpStatus.NOT_FOUND
        );

        assertEquals(
                "W18_TASK_NOT_FOUND",
                missingTaskProblem.path("code").asText()
        );
    }

    private JsonNode postAction(
            String path,
            HttpStatus expectedStatus
    ) {
        return post(
                path,
                Map.of(),
                expectedStatus
        );
    }

    private JsonNode post(
            String path,
            Object request,
            HttpStatus expectedStatus
    ) {
        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity(
                        path,
                        request,
                        JsonNode.class
                );

        assertEquals(
                expectedStatus.value(),
                response.getStatusCode().value(),
                path
        );

        JsonNode body = response.getBody();
        assertNotNull(body, path + " returned no body");

        return body;
    }

    private JsonNode get(
            String path,
            HttpStatus expectedStatus
    ) {
        ResponseEntity<JsonNode> response =
                restTemplate.getForEntity(
                        path,
                        JsonNode.class
                );

        assertEquals(
                expectedStatus.value(),
                response.getStatusCode().value(),
                path
        );

        JsonNode body = response.getBody();
        assertNotNull(body, path + " returned no body");

        return body;
    }
}