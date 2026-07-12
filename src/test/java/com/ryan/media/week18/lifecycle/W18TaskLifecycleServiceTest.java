package com.ryan.media.week18.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class W18TaskLifecycleServiceTest {

    private W18TaskLifecycleService service;

    @BeforeEach
    void setUp() throws Exception {
        Path contract = Path.of(
                "artifacts/week18/"
                        + "w18_selector_repair_handoff_20260708.json"
        );

        assertTrue(
                Files.isRegularFile(contract),
                "Missing Java copy of Mainbase handoff contract"
        );

        service = new W18TaskLifecycleService(
                new ObjectMapper(),
                contract
        );
    }

    @Test
    void winnerAndRepairProbeFollowDifferentRealArtifactRoutes() {
        W18TaskLifecycleService.Task winner = service.create(
                "glass_drop_room_001",
                W18TaskLifecycleService.ArtifactKind.WINNER,
                "glass_drop_room_001|dss_layer_avoid"
        );

        assertEquals(
                W18TaskLifecycleService.State.CREATED,
                winner.state()
        );
        assertEquals(
                W18TaskLifecycleService.State.QUEUED,
                service.queue(winner.taskId()).state()
        );
        assertEquals(
                W18TaskLifecycleService.State.RUNNING,
                service.start(winner.taskId()).state()
        );
        assertEquals(
                W18TaskLifecycleService.State.SUCCEEDED,
                service.decide(winner.taskId()).state()
        );

        W18TaskLifecycleService.Task winnerBound =
                service.bindResult(winner.taskId());

        assertEquals(
                W18TaskLifecycleService.State.RESULT_BOUND,
                winnerBound.state()
        );
        assertTrue(
                winnerBound.boundArtifactPath()
                        .contains("w18_010_glass_drop_room_001")
        );

        W18TaskLifecycleService.Task repair = service.create(
                "glass_drop_room_001",
                W18TaskLifecycleService.ArtifactKind.REPAIR_PROBE,
                "mr_001"
        );

        service.queue(repair.taskId());
        service.start(repair.taskId());

        assertEquals(
                W18TaskLifecycleService.State.REPAIR_REQUIRED,
                service.decide(repair.taskId()).state()
        );
        assertEquals(
                W18TaskLifecycleService.State.REPAIR_APPLIED,
                service.applyRepair(repair.taskId()).state()
        );

        W18TaskLifecycleService.Task repairBound =
                service.bindResult(repair.taskId());

        assertEquals(
                W18TaskLifecycleService.State.RESULT_BOUND,
                repairBound.state()
        );
        assertTrue(
                repairBound.boundArtifactPath().contains(
                        "silence_trim_or_gain__after.wav"
                )
        );

        ObjectNode winnerCard = service.resultCard(winner.taskId());

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

        ObjectNode report = service.lifecycleReport();

        assertEquals(2, report.path("taskCount").asInt());
        assertEquals(
                1,
                report.path("repairRequiredCount").asInt()
        );
        assertEquals(
                1,
                report.path("succeededCount").asInt()
        );
        assertEquals(
                1,
                report.path("repairAppliedCount").asInt()
        );
        assertEquals(
                2,
                report.path("resultBoundCount").asInt()
        );
    }

    @Test
    void rejectsUnknownArtifactsAndIllegalTransitions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        "unknown_case",
                        W18TaskLifecycleService.ArtifactKind.WINNER,
                        "missing_candidate"
                )
        );

        W18TaskLifecycleService.Task task = service.create(
                "street_rain_crosswalk_001",
                W18TaskLifecycleService.ArtifactKind.WINNER,
                "street_rain_crosswalk_001|dss_event_timeline"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.start(task.taskId())
        );

        assertTrue(
                exception.getMessage().contains("Illegal transition")
        );
        assertEquals(
                W18TaskLifecycleService.State.CREATED,
                service.get(task.taskId()).state()
        );
    }
}