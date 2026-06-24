#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
from datetime import datetime, timezone
from pathlib import Path


JAVA_REPO = Path(__file__).resolve().parents[1]
MAINBASE = Path.home() / "work/audio_engineering_repo_skeleton_v1"

SRC_MANIFEST = MAINBASE / "artifacts/evals/week17_layer_mix_v0_manifest.json"
PAYLOAD_OUT = JAVA_REPO / "artifacts/manifests/week17_layer_mix_v0_result_mainbase_payload.json"
REPORT_OUT = JAVA_REPO / "artifacts/manifests/week17_layer_mix_v0_result_preview_api_report.json"
DOC_OUT = JAVA_REPO / "docs/api/week17-layer-mix-v0-result-preview.md"

CONTROLLER_OUT = JAVA_REPO / "src/main/java/com/ryan/media/controller/Week17LayerMixV0ResultPreviewController.java"
TEST_OUT = JAVA_REPO / "src/test/java/com/ryan/media/controller/Week17LayerMixV0ResultPreviewIT.java"


def git_head(repo: Path) -> str:
    return subprocess.check_output(
        ["git", "-C", str(repo), "rev-parse", "--short", "HEAD"],
        text=True,
    ).strip()


def require(condition: bool, msg: str) -> None:
    if not condition:
        raise RuntimeError(msg)


def java_bool(v: bool) -> str:
    return "true" if v else "false"


def java_string_list(values: list[str]) -> str:
    return "List.of(" + ", ".join(f'"{v}"' for v in values) + ")"


def main() -> int:
    require(SRC_MANIFEST.exists(), f"Mainbase manifest missing: {SRC_MANIFEST}")

    data = json.loads(SRC_MANIFEST.read_text(encoding="utf-8"))

    required_exact = {
        "decision": "PASS_WEEK17_LAYER_MIX_V0_PLACEHOLDER_CONTROL",
        "trackTotal": 7,
        "placeholderInputOnly": True,
        "realCandidateAudioClaimed": False,
        "semanticAudioQualityPassClaimed": False,
        "humanReviewPassClaimed": False,
        "finalMixReadinessClaimed": False,
        "productionMixerAvailabilityClaimed": False,
    }
    for k, expected in required_exact.items():
        require(data.get(k) == expected, f"manifest field mismatch: {k}={data.get(k)!r}, expected={expected!r}")

    require(data.get("selectedControlIds") == ["0001", "0002", "0003", "0005", "0006", "0008", "0009"], "unexpected selectedControlIds")
    require(data.get("blockedInputIds") == ["0004", "0007", "0010"], "unexpected blockedInputIds")
    require(float(data.get("finalRms", 0.0)) > 0.0, "finalRms must be > 0")
    require(float(data.get("finalClipRateBeforeClip", 1.0)) == 0.0, "finalClipRateBeforeClip must be 0")

    mainbase_head = git_head(MAINBASE)
    java_head = git_head(JAVA_REPO)

    PAYLOAD_OUT.parent.mkdir(parents=True, exist_ok=True)
    REPORT_OUT.parent.mkdir(parents=True, exist_ok=True)
    DOC_OUT.parent.mkdir(parents=True, exist_ok=True)
    CONTROLLER_OUT.parent.mkdir(parents=True, exist_ok=True)
    TEST_OUT.parent.mkdir(parents=True, exist_ok=True)

    payload = {
        "schemaVersion": "week17.layer_mix_v0.result_preview_payload.v1",
        "sourceMainbaseHead": mainbase_head,
        "sourceMainbaseManifest": "artifacts/evals/week17_layer_mix_v0_manifest.json",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "decision": data["decision"],
        "trackTotal": data["trackTotal"],
        "selectedControlIds": data["selectedControlIds"],
        "blockedInputIds": data["blockedInputIds"],
        "mixArtifactPath": data["mixArtifactPath"],
        "finalPeak": data["finalPeak"],
        "finalRms": data["finalRms"],
        "finalClipRateBeforeClip": data["finalClipRateBeforeClip"],
        "placeholderInputOnly": data["placeholderInputOnly"],
        "realCandidateAudioClaimed": data["realCandidateAudioClaimed"],
        "semanticAudioQualityPassClaimed": data["semanticAudioQualityPassClaimed"],
        "humanReviewPassClaimed": data["humanReviewPassClaimed"],
        "finalMixReadinessClaimed": data["finalMixReadinessClaimed"],
        "productionMixerAvailabilityClaimed": data["productionMixerAvailabilityClaimed"],
        "blockedClaims": data.get("blockedClaims", []),
        "previewApiPath": "/api/week17/layer-mix-v0/result-preview",
        "platformConsumable": True,
        "readOnlyPreview": True,
        "realWorkerTriggered": False,
        "databasePersistenceClaimed": False,
    }

    PAYLOAD_OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")

    controller = f'''package com.ryan.media.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Week17LayerMixV0ResultPreviewController {{

    @GetMapping("/api/week17/layer-mix-v0/result-preview")
    public Map<String, Object> getLayerMixV0ResultPreview() {{
        return Map.ofEntries(
            Map.entry("schemaVersion", "week17.layer_mix_v0.result_preview_api.v1"),
            Map.entry("sourceMainbaseHead", "{mainbase_head}"),
            Map.entry("sourceMainbaseManifest", "artifacts/evals/week17_layer_mix_v0_manifest.json"),
            Map.entry("decision", "{payload["decision"]}"),
            Map.entry("trackTotal", {payload["trackTotal"]}),
            Map.entry("selectedControlIds", {java_string_list(payload["selectedControlIds"])}),
            Map.entry("blockedInputIds", {java_string_list(payload["blockedInputIds"])}),
            Map.entry("mixArtifactPath", "{payload["mixArtifactPath"]}"),
            Map.entry("finalPeak", {payload["finalPeak"]}),
            Map.entry("finalRms", {payload["finalRms"]}),
            Map.entry("finalClipRateBeforeClip", {payload["finalClipRateBeforeClip"]}),
            Map.entry("placeholderInputOnly", {java_bool(payload["placeholderInputOnly"])}),
            Map.entry("realCandidateAudioClaimed", {java_bool(payload["realCandidateAudioClaimed"])}),
            Map.entry("semanticAudioQualityPassClaimed", {java_bool(payload["semanticAudioQualityPassClaimed"])}),
            Map.entry("humanReviewPassClaimed", {java_bool(payload["humanReviewPassClaimed"])}),
            Map.entry("finalMixReadinessClaimed", {java_bool(payload["finalMixReadinessClaimed"])}),
            Map.entry("productionMixerAvailabilityClaimed", {java_bool(payload["productionMixerAvailabilityClaimed"])}),
            Map.entry("blockedClaims", {java_string_list(payload["blockedClaims"])}),
            Map.entry("platformConsumable", true),
            Map.entry("readOnlyPreview", true),
            Map.entry("realWorkerTriggered", false),
            Map.entry("databasePersistenceClaimed", false)
        );
    }}
}}
'''
    CONTROLLER_OUT.write_text(controller, encoding="utf-8")

    test = f'''package com.ryan.media.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
class Week17LayerMixV0ResultPreviewIT {{

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeLayerMixV0ResultPreview() throws Exception {{
        mockMvc.perform(get("/api/week17/layer-mix-v0/result-preview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.schemaVersion").value("week17.layer_mix_v0.result_preview_api.v1"))
            .andExpect(jsonPath("$.sourceMainbaseHead").value("{mainbase_head}"))
            .andExpect(jsonPath("$.decision").value("PASS_WEEK17_LAYER_MIX_V0_PLACEHOLDER_CONTROL"))
            .andExpect(jsonPath("$.trackTotal").value(7))
            .andExpect(jsonPath("$.selectedControlIds[0]").value("0001"))
            .andExpect(jsonPath("$.selectedControlIds[6]").value("0009"))
            .andExpect(jsonPath("$.blockedInputIds[0]").value("0004"))
            .andExpect(jsonPath("$.blockedInputIds[2]").value("0010"))
            .andExpect(jsonPath("$.mixArtifactPath").value("artifacts/audio/week17_layer_mix_v0/week17_layer_mix_v0_placeholder_control_mix.wav"))
            .andExpect(jsonPath("$.finalClipRateBeforeClip").value(0.0))
            .andExpect(jsonPath("$.placeholderInputOnly").value(true))
            .andExpect(jsonPath("$.realCandidateAudioClaimed").value(false))
            .andExpect(jsonPath("$.semanticAudioQualityPassClaimed").value(false))
            .andExpect(jsonPath("$.humanReviewPassClaimed").value(false))
            .andExpect(jsonPath("$.finalMixReadinessClaimed").value(false))
            .andExpect(jsonPath("$.productionMixerAvailabilityClaimed").value(false))
            .andExpect(jsonPath("$.platformConsumable").value(true))
            .andExpect(jsonPath("$.readOnlyPreview").value(true))
            .andExpect(jsonPath("$.realWorkerTriggered").value(false))
            .andExpect(jsonPath("$.databasePersistenceClaimed").value(false));
    }}
}}
'''
    TEST_OUT.write_text(test, encoding="utf-8")

    report = {
        "schemaVersion": "week17.layer_mix_v0.result_preview_api_report.v1",
        "generatedAtUtc": datetime.now(timezone.utc).isoformat(),
        "decision": "READY_TO_RUN_FOCUSED_IT",
        "javaHeadBeforeCommit": java_head,
        "sourceMainbaseHead": mainbase_head,
        "apiPath": "/api/week17/layer-mix-v0/result-preview",
        "createdFiles": [
            str(CONTROLLER_OUT.relative_to(JAVA_REPO)),
            str(TEST_OUT.relative_to(JAVA_REPO)),
            str(PAYLOAD_OUT.relative_to(JAVA_REPO)),
            str(REPORT_OUT.relative_to(JAVA_REPO)),
            str(DOC_OUT.relative_to(JAVA_REPO)),
        ],
        "mainbaseDecision": data["decision"],
        "trackTotal": data["trackTotal"],
        "mixArtifactPath": data["mixArtifactPath"],
        "finalClipRateBeforeClip": data["finalClipRateBeforeClip"],
        "platformConsumable": True,
        "readOnlyPreview": True,
        "realWorkerTriggered": False,
        "databasePersistenceClaimed": False,
        "blockedClaimsPreserved": True,
    }
    REPORT_OUT.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    DOC_OUT.write_text(
        "\\n".join([
            "# Week17 Layer Mix V0 Result Preview API",
            "",
            "## Endpoint",
            "",
            "`GET /api/week17/layer-mix-v0/result-preview`",
            "",
            "## Purpose",
            "",
            "Expose the committed Mainbase Week17 layer mixer v0 placeholder-control result as a read-only Java platform preview contract.",
            "",
            "## Source",
            "",
            f"- Mainbase HEAD: `{mainbase_head}`",
            "- Source manifest: `artifacts/evals/week17_layer_mix_v0_manifest.json`",
            f"- Mix artifact: `{payload['mixArtifactPath']}`",
            "",
            "## Boundary",
            "",
            "- This API does not trigger a real worker.",
            "- This API does not persist the result into a database.",
            "- This API does not claim semantic audio quality pass.",
            "- This API does not claim human review pass.",
            "- This API does not claim final mix readiness.",
            "- This API does not claim production mixer availability.",
            "",
        ]),
        encoding="utf-8",
    )

    print(json.dumps({
        "decision": "GENERATED_WEEK17_LAYER_MIX_V0_RESULT_PREVIEW_API",
        "sourceMainbaseHead": mainbase_head,
        "apiPath": "/api/week17/layer-mix-v0/result-preview",
        "trackTotal": payload["trackTotal"],
        "finalClipRateBeforeClip": payload["finalClipRateBeforeClip"],
        "createdFiles": report["createdFiles"],
    }, ensure_ascii=False, indent=2))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())