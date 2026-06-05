#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import re
import subprocess
from pathlib import Path
from typing import Any, Dict


JAVA_ROOT = Path(__file__).resolve().parents[1]
CLOUD_ROOT = Path(os.environ.get(
    "CLOUD_PATH",
    str(Path.home() / "work/ai-job-platform-cloud")
)).expanduser().resolve()

CLOUD_RUNTIME_INDEX = CLOUD_ROOT / "artifacts/manifests/week12_cloud_mainbase_audio_timing_runtime_index.json"

RESOURCE_DIR = JAVA_ROOT / "src/main/resources/week12"
RESOURCE_JSON = RESOURCE_DIR / "week12_cloud_mainbase_audio_timing_runtime_index.json"

OUT_REPORT = JAVA_ROOT / "artifacts/manifests/week12_java_audio_timing_runtime_contract_report.json"


def run_git(repo: Path, *args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(repo), *args],
        text=True,
        stderr=subprocess.STDOUT,
    ).strip()


def load_json(path: Path) -> Dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"missing required file: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def find_application_package() -> str:
    candidates = list((JAVA_ROOT / "src/main/java").rglob("*.java"))
    for path in candidates:
        text = path.read_text(encoding="utf-8", errors="replace")
        if "@SpringBootApplication" in text:
            m = re.search(r"^\s*package\s+([a-zA-Z0-9_.]+)\s*;", text, flags=re.MULTILINE)
            if not m:
                raise RuntimeError(f"SpringBootApplication found but package missing: {path}")
            return m.group(1)
    raise RuntimeError("Cannot find @SpringBootApplication package")


def package_to_path(pkg: str) -> Path:
    return Path(*pkg.split("."))


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def main() -> int:
    runtime_index = load_json(CLOUD_RUNTIME_INDEX)

    if runtime_index.get("status") != "PASS":
        raise RuntimeError(f"Cloud runtime index is not PASS: {runtime_index.get('status')}")

    metrics = runtime_index.get("metrics", {})
    offsets = runtime_index.get("eventLocalPlacementOffsets", [])

    assert metrics.get("candidateCount") == 10
    assert metrics.get("timingBoundCount") == 10
    assert metrics.get("alignmentPassCount") == 10
    assert metrics.get("assetTimeModeCounts", {}).get("full_clip") == 5
    assert metrics.get("assetTimeModeCounts", {}).get("event_local") == 5
    assert len(offsets) == 5
    assert runtime_index.get("blockers") == []

    app_pkg = find_application_package()
    week12_pkg = app_pkg + ".week12"
    main_dir = JAVA_ROOT / "src/main/java" / package_to_path(week12_pkg)
    test_dir = JAVA_ROOT / "src/test/java" / package_to_path(week12_pkg)

    RESOURCE_DIR.mkdir(parents=True, exist_ok=True)
    RESOURCE_JSON.write_text(json.dumps(runtime_index, ensure_ascii=False, indent=2), encoding="utf-8")

    controller = f'''package {week12_pkg};

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week12AudioTimingRuntimeContractController {{

    private final ObjectMapper objectMapper;

    public Week12AudioTimingRuntimeContractController(ObjectMapper objectMapper) {{
        this.objectMapper = objectMapper;
    }}

    @GetMapping("/api/week12/audio-timing-runtime")
    public ResponseEntity<JsonNode> runtimeContract() throws IOException {{
        return ResponseEntity.ok(loadRuntimeIndex());
    }}

    @GetMapping("/api/week12/audio-timing-runtime/event-local-offsets")
    public ResponseEntity<JsonNode> eventLocalOffsets() throws IOException {{
        return ResponseEntity.ok(loadRuntimeIndex().path("eventLocalPlacementOffsets"));
    }}

    @GetMapping("/api/week12/audio-timing-runtime/placement-required")
    public ResponseEntity<JsonNode> placementRequired() throws IOException {{
        return ResponseEntity.ok(loadRuntimeIndex().path("runtimeSemantics").path("eventLocalMode"));
    }}

    private JsonNode loadRuntimeIndex() throws IOException {{
        ClassPathResource resource = new ClassPathResource("week12/week12_cloud_mainbase_audio_timing_runtime_index.json");
        return objectMapper.readTree(resource.getInputStream());
    }}
}}
'''

    test = f'''package {week12_pkg};

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Week12AudioTimingRuntimeContractIT {{

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesCloudConsumedAudioTimingRuntimeContract() {{
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/week12/audio-timing-runtime",
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("status").asText()).isEqualTo("PASS");
        assertThat(body.path("source").path("mainbaseCommit").asText()).isEqualTo("28e79ff");

        JsonNode metrics = body.path("metrics");
        assertThat(metrics.path("candidateCount").asInt()).isEqualTo(10);
        assertThat(metrics.path("timingBoundCount").asInt()).isEqualTo(10);
        assertThat(metrics.path("alignmentPassCount").asInt()).isEqualTo(10);
        assertThat(metrics.path("alignmentFailCount").asInt()).isEqualTo(0);
        assertThat(metrics.path("assetTimeModeCounts").path("full_clip").asInt()).isEqualTo(5);
        assertThat(metrics.path("assetTimeModeCounts").path("event_local").asInt()).isEqualTo(5);

        JsonNode offsets = body.path("eventLocalPlacementOffsets");
        assertThat(offsets.isArray()).isTrue();
        assertThat(offsets).hasSize(5);

        for (JsonNode offset : offsets) {{
            assertThat(offset.path("placementRequired").asBoolean()).isTrue();
            assertThat(offset.path("expectedStartSec").asText()).isNotBlank();
            assertThat(offset.path("expectedEndSec").asText()).isNotBlank();
            assertThat(offset.path("peakGlobalSec").asText()).isNotBlank();
        }}

        assertThat(body.path("runtimeWarnings").toString())
            .contains("EVENT_LOCAL_ASSETS_REQUIRE_EXPECTED_START_SEC_PLACEMENT");
    }}

    @Test
    void exposesPlacementRequiredSemanticsForEventLocalAssets() {{
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/week12/audio-timing-runtime/placement-required",
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.path("assetTimeMode").asText()).isEqualTo("event_local");
        assertThat(body.path("requiredForMixer").asBoolean()).isTrue();
        assertThat(body.path("requiredForDashboard").asBoolean()).isTrue();
        assertThat(body.path("placement").asText()).contains("expectedStartSec");
    }}

    @Test
    void exposesExactlyFiveEventLocalPlacementOffsets() {{
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/week12/audio-timing-runtime/event-local-offsets",
            JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(5);

        assertThat(body.get(0).path("candidateId").asText()).startsWith("procedural_v0_");
        assertThat(body.get(0).path("eventId").asText()).isEqualTo("evt_002");
        assertThat(body.get(0).path("placementRequired").asBoolean()).isTrue();
    }}
}}
'''

    controller_path = main_dir / "Week12AudioTimingRuntimeContractController.java"
    test_path = test_dir / "Week12AudioTimingRuntimeContractIT.java"

    write(controller_path, controller)
    write(test_path, test)

    report = {
        "status": "GENERATED",
        "scope": "java_exposes_week12_audio_timing_runtime_contract",
        "applicationPackage": app_pkg,
        "week12Package": week12_pkg,
        "source": {
            "cloudPath": str(CLOUD_ROOT),
            "cloudCommit": run_git(CLOUD_ROOT, "rev-parse", "--short", "HEAD"),
            "cloudOriginMain": run_git(CLOUD_ROOT, "rev-parse", "--short", "origin/main"),
            "cloudRuntimeIndex": str(CLOUD_RUNTIME_INDEX),
        },
        "generated": {
            "resource": str(RESOURCE_JSON.relative_to(JAVA_ROOT)),
            "controller": str(controller_path.relative_to(JAVA_ROOT)),
            "integrationTest": str(test_path.relative_to(JAVA_ROOT)),
        },
        "contractFields": [
            "assetTimeModeCounts",
            "eventLocalPlacementOffsets",
            "placementRequired",
            "expectedStartSec",
            "expectedEndSec",
            "peakGlobalSec",
            "runtimeWarnings",
        ],
        "boundaryStatement": (
            "Java exposes Cloud-consumed Mainbase timing semantics as a week12 HTTP contract. "
            "This does not implement a durable artifact registry, mixer, worker, semantic quality validation, or human audition."
        ),
    }

    OUT_REPORT.parent.mkdir(parents=True, exist_ok=True)
    OUT_REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())