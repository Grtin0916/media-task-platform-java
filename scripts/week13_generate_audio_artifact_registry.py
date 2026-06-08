#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path


JAVA_ROOT = Path.cwd()
MAINBASE_ROOT = Path.home() / "work/audio_engineering_repo_skeleton_v1"

MAINBASE_PLACEMENT_TABLE = MAINBASE_ROOT / "artifacts/evals/week13_mix_global_placement_table.csv"
MAINBASE_DRYRUN_MANIFEST = MAINBASE_ROOT / "artifacts/audio_mix/week13_mix_preview_manifest.json"

FIXTURE_PLACEMENT_TABLE = JAVA_ROOT / "artifacts/fixtures/week13_mainbase_mix_global_placement_table.csv"
FIXTURE_DRYRUN_MANIFEST = JAVA_ROOT / "artifacts/fixtures/week13_mainbase_mix_preview_manifest.json"

SRC_DIR = JAVA_ROOT / "src/main/java/com/ryan/media/week13"
TEST_DIR = JAVA_ROOT / "src/test/java/com/ryan/media/week13"
DOC_PATH = JAVA_ROOT / "docs/api/week13-audio-artifact-registry-contract.md"
REPORT_WRITER = JAVA_ROOT / "scripts/week13_write_audio_artifact_registry_report.py"


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        raise FileNotFoundError(f"missing required Mainbase placement table: {path}")
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def jstr(s: str | None) -> str:
    if s is None:
        return '""'
    return json.dumps(str(s), ensure_ascii=False)


def jbool(s: str | bool | None) -> str:
    if isinstance(s, bool):
        return "true" if s else "false"
    return "true" if str(s).lower() == "true" else "false"


def jbd(s: str | None) -> str:
    if s is None or str(s).strip() == "":
        return 'new BigDecimal("0")'
    return f'new BigDecimal("{str(s).strip()}")'


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def main() -> int:
    if not MAINBASE_DRYRUN_MANIFEST.exists():
        raise FileNotFoundError(f"missing required Mainbase dry-run manifest: {MAINBASE_DRYRUN_MANIFEST}")

    rows = read_csv(MAINBASE_PLACEMENT_TABLE)
    if len(rows) != 10:
        raise RuntimeError(f"expected 10 Mainbase placement rows, got {len(rows)}")

    fixture_manifest = json.loads(MAINBASE_DRYRUN_MANIFEST.read_text(encoding="utf-8"))
    if fixture_manifest.get("status") != "PASS":
        raise RuntimeError("Mainbase dry-run manifest is not PASS")

    SRC_DIR.mkdir(parents=True, exist_ok=True)
    TEST_DIR.mkdir(parents=True, exist_ok=True)
    FIXTURE_PLACEMENT_TABLE.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(MAINBASE_PLACEMENT_TABLE, FIXTURE_PLACEMENT_TABLE)
    shutil.copy2(MAINBASE_DRYRUN_MANIFEST, FIXTURE_DRYRUN_MANIFEST)

    items_java = []
    for r in rows:
        items_java.append(
            "new AudioArtifactRegistryItem("
            f"{jstr(r.get('candidateId'))}, "
            f"{jstr(r.get('audioUri'))}, "
            f"{jstr(r.get('sourceType'))}, "
            f"{jstr(r.get('caseId'))}, "
            f"{jstr(r.get('sceneId'))}, "
            f"{jstr(r.get('eventId'))}, "
            f"{jstr(r.get('layer'))}, "
            f"{jstr(r.get('label'))}, "
            f"{jstr(r.get('assetTimeMode'))}, "
            f"{jbool(r.get('placementRequired'))}, "
            f"{jbd(r.get('expectedStartSec'))}, "
            f"{jbd(r.get('expectedEndSec'))}, "
            f"{jbd(r.get('globalStartSec'))}, "
            f"{jbd(r.get('globalEndSec'))}, "
            f"{jbd(r.get('placementOffsetSec'))}, "
            f"{jstr('READY_FOR_RUNTIME_PLACEMENT')}"
            ")"
        )

    items_block = ",\n                ".join(items_java)

    write(
        SRC_DIR / "AudioArtifactRegistryItem.java",
        """package com.ryan.media.week13;

import java.math.BigDecimal;

public record AudioArtifactRegistryItem(
        String candidateId,
        String audioUri,
        String sourceType,
        String caseId,
        String sceneId,
        String eventId,
        String layer,
        String label,
        String assetTimeMode,
        boolean placementRequired,
        BigDecimal expectedStartSec,
        BigDecimal expectedEndSec,
        BigDecimal globalStartSec,
        BigDecimal globalEndSec,
        BigDecimal placementOffsetSec,
        String status
) {
}
""",
    )

    write(
        SRC_DIR / "AudioArtifactRegistryResponse.java",
        """package com.ryan.media.week13;

import java.util.List;
import java.util.Map;

public record AudioArtifactRegistryResponse(
        String status,
        String scope,
        int candidateCount,
        Map<String, Long> assetTimeModeCounts,
        long placementRequiredCount,
        long readyCount,
        List<String> contractFields,
        String boundaryStatement,
        List<AudioArtifactRegistryItem> items
) {
}
""",
    )

    write(
        SRC_DIR / "AudioArtifactRegistryService.java",
        f"""package com.ryan.media.week13;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AudioArtifactRegistryService {{

    private final List<AudioArtifactRegistryItem> items = List.of(
                {items_block}
    );

    public AudioArtifactRegistryResponse list() {{
        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(AudioArtifactRegistryItem::assetTimeMode, Collectors.counting()));

        long placementRequiredCount = items.stream()
                .filter(AudioArtifactRegistryItem::placementRequired)
                .count();

        long readyCount = items.stream()
                .filter(item -> "READY_FOR_RUNTIME_PLACEMENT".equals(item.status()))
                .count();

        return new AudioArtifactRegistryResponse(
                "GENERATED",
                "week13_audio_artifact_registry_contract_v0",
                items.size(),
                counts,
                placementRequiredCount,
                readyCount,
                List.of(
                        "candidateId",
                        "audioUri",
                        "sourceType",
                        "assetTimeMode",
                        "expectedStartSec",
                        "expectedEndSec",
                        "globalStartSec",
                        "globalEndSec",
                        "placementOffsetSec",
                        "placementRequired",
                        "status"
                ),
                "Java exposes Mainbase Week13 placement/dry-run candidate artifacts as a runtime registry contract. "
                        + "This is not durable object storage, not a final mixer, not semantic quality validation, "
                        + "not human audition, and not production readiness.",
                items
        );
    }}

    public Optional<AudioArtifactRegistryItem> findByCandidateId(String candidateId) {{
        return items.stream()
                .filter(item -> item.candidateId().equals(candidateId))
                .findFirst();
    }}
}}
""",
    )

    write(
        SRC_DIR / "AudioArtifactRegistryController.java",
        """package com.ryan.media.week13;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/week13/audio-artifacts")
public class AudioArtifactRegistryController {

    private final AudioArtifactRegistryService service;

    public AudioArtifactRegistryController(AudioArtifactRegistryService service) {
        this.service = service;
    }

    @GetMapping
    public AudioArtifactRegistryResponse list() {
        return service.list();
    }

    @GetMapping("/{candidateId}")
    public ResponseEntity<AudioArtifactRegistryItem> getOne(@PathVariable String candidateId) {
        return service.findByCandidateId(candidateId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
""",
    )

    write(
        TEST_DIR / "Week13AudioArtifactRegistryTest.java",
        """package com.ryan.media.week13;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class Week13AudioArtifactRegistryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "week13", roles = {"USER"})
    void shouldExposeMainbaseWeek13PlacementRegistryContract() throws Exception {
        String body = mockMvc.perform(get("/api/week13/audio-artifacts"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(body);

        assertEquals("GENERATED", root.get("status").asText());
        assertEquals("week13_audio_artifact_registry_contract_v0", root.get("scope").asText());
        assertEquals(10, root.get("candidateCount").asInt());
        assertEquals(5, root.get("assetTimeModeCounts").get("full_clip").asInt());
        assertEquals(5, root.get("assetTimeModeCounts").get("event_local").asInt());
        assertEquals(5, root.get("placementRequiredCount").asInt());
        assertEquals(10, root.get("readyCount").asInt());

        int eventLocalCount = 0;
        int fixedPlacementMismatchCount = 0;
        for (JsonNode item : root.get("items")) {
            assertTrue(item.hasNonNull("candidateId"));
            assertTrue(item.hasNonNull("audioUri"));
            assertTrue(item.hasNonNull("assetTimeMode"));
            assertTrue(item.hasNonNull("expectedStartSec"));
            assertTrue(item.hasNonNull("globalStartSec"));
            assertEquals("READY_FOR_RUNTIME_PLACEMENT", item.get("status").asText());

            if ("event_local".equals(item.get("assetTimeMode").asText())) {
                eventLocalCount++;
                assertTrue(item.get("placementRequired").asBoolean());
                BigDecimal expectedStart = new BigDecimal(item.get("expectedStartSec").asText());
                BigDecimal globalStart = new BigDecimal(item.get("globalStartSec").asText());
                BigDecimal offset = new BigDecimal(item.get("placementOffsetSec").asText());
                if (expectedStart.compareTo(globalStart) != 0 || expectedStart.compareTo(offset) != 0) {
                    fixedPlacementMismatchCount++;
                }
            }

            if ("full_clip".equals(item.get("assetTimeMode").asText())) {
                assertFalse(item.get("placementRequired").asBoolean());
                assertEquals(0, new BigDecimal(item.get("globalStartSec").asText()).compareTo(BigDecimal.ZERO));
            }
        }

        assertEquals(5, eventLocalCount);
        assertEquals(0, fixedPlacementMismatchCount);
    }

    @Test
    @WithMockUser(username = "week13", roles = {"USER"})
    void shouldReturnSingleCandidateById() throws Exception {
        String body = mockMvc.perform(get("/api/week13/audio-artifacts/procedural_v0_0002"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode item = objectMapper.readTree(body);
        assertEquals("procedural_v0_0002", item.get("candidateId").asText());
        assertEquals("event_local", item.get("assetTimeMode").asText());
        assertTrue(item.get("placementRequired").asBoolean());
        assertEquals(0, new BigDecimal(item.get("expectedStartSec").asText())
                .compareTo(new BigDecimal(item.get("globalStartSec").asText())));
    }

    @Test
    @WithMockUser(username = "week13", roles = {"USER"})
    void shouldReturnNotFoundForUnknownCandidate() throws Exception {
        mockMvc.perform(get("/api/week13/audio-artifacts/not-a-real-candidate"))
                .andExpect(status().isNotFound());
    }
}
""",
    )

    write(
        DOC_PATH,
        """# Week13 Audio Artifact Registry Contract v0

## Scope

This contract exposes Mainbase Week13 mix placement and dry-run evidence as Java API data.

Endpoint:

- `GET /api/week13/audio-artifacts`
- `GET /api/week13/audio-artifacts/{candidateId}`

## Required fields

- `candidateId`
- `audioUri`
- `sourceType`
- `assetTimeMode`
- `expectedStartSec`
- `expectedEndSec`
- `globalStartSec`
- `globalEndSec`
- `placementOffsetSec`
- `placementRequired`
- `status`

## Runtime rule

- `full_clip`: `globalStartSec = 0`
- `event_local`: `globalStartSec = expectedStartSec`

## Boundary

This is an in-memory API contract over committed Mainbase Week13 evidence.

It does not implement durable artifact storage, database-backed registry, final mixer behavior, semantic quality validation, human audition, or production readiness.
""",
    )

    write(
        REPORT_WRITER,
        """#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path.cwd()
LOG_PATH = ROOT / "artifacts/logs/week13_audio_artifact_registry_it.log"
REPORT_PATH = ROOT / "artifacts/manifests/week13_java_audio_artifact_registry_contract_report.json"
FIXTURE_PATH = ROOT / "artifacts/fixtures/week13_mainbase_mix_preview_manifest.json"


def git_head() -> str:
    try:
        return subprocess.check_output(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT, text=True).strip()
    except Exception:
        return "UNKNOWN"


def main() -> int:
    log_text = LOG_PATH.read_text(encoding="utf-8", errors="replace") if LOG_PATH.exists() else ""
    fixture = json.loads(FIXTURE_PATH.read_text(encoding="utf-8")) if FIXTURE_PATH.exists() else {}

    test_passed = "BUILD SUCCESS" in log_text
    status = "PASS" if test_passed else "FAIL"

    report = {
        "status": status,
        "scope": "java_exposes_week13_audio_artifact_registry_contract_v0",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "sourceRepo": {
            "path": str(ROOT),
            "head": git_head(),
        },
        "mainbaseEvidence": {
            "dryRunStatus": fixture.get("status"),
            "candidateCount": fixture.get("candidateCount"),
            "assetTimeModeCounts": fixture.get("assetTimeModeCounts"),
            "fixedPlacementMisplacedCount": fixture.get("fixedPlacementMisplacedCount"),
            "naiveZeroWouldMisplaceCount": fixture.get("naiveZeroWouldMisplaceCount"),
        },
        "endpoints": [
            "GET /api/week13/audio-artifacts",
            "GET /api/week13/audio-artifacts/{candidateId}",
        ],
        "contractFields": [
            "candidateId",
            "audioUri",
            "sourceType",
            "assetTimeMode",
            "expectedStartSec",
            "expectedEndSec",
            "globalStartSec",
            "globalEndSec",
            "placementOffsetSec",
            "placementRequired",
            "status",
        ],
        "test": {
            "name": "Week13AudioArtifactRegistryTest",
            "log": "artifacts/logs/week13_audio_artifact_registry_it.log",
            "passed": test_passed,
        },
        "boundaryStatement": (
            "Java exposes Mainbase Week13 placement/dry-run candidate artifacts as an in-memory API contract. "
            "This does not implement durable object storage, database-backed registry, final mixer, semantic quality validation, "
            "human audition, or production readiness."
        ),
        "blockers": [] if test_passed else ["WEEK13_AUDIO_ARTIFACT_REGISTRY_IT_NOT_PASS"],
    }

    REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
    REPORT_PATH.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if status == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
""",
    )
    REPORT_WRITER.chmod(0o755)

    print(json.dumps({
        "status": "GENERATED",
        "rows": len(rows),
        "sourceMainbaseDryrunStatus": fixture_manifest.get("status"),
        "written": [
            str(SRC_DIR / "AudioArtifactRegistryItem.java"),
            str(SRC_DIR / "AudioArtifactRegistryResponse.java"),
            str(SRC_DIR / "AudioArtifactRegistryService.java"),
            str(SRC_DIR / "AudioArtifactRegistryController.java"),
            str(TEST_DIR / "Week13AudioArtifactRegistryTest.java"),
            str(DOC_PATH),
            str(REPORT_WRITER),
            str(FIXTURE_PLACEMENT_TABLE),
            str(FIXTURE_DRYRUN_MANIFEST),
        ],
    }, ensure_ascii=False, indent=2))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())