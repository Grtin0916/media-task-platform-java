package com.ryan.media.week13;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AudioArtifactRegistryService {

    private final List<AudioArtifactRegistryItem> items = List.of(
                new AudioArtifactRegistryItem("procedural_v0_0001", "artifacts/audio_candidates/week12_procedural_baseline_v0/0001_slot_v0_0001_generation_fallback.wav", "procedural_baseline_v0", "seed_0001_case_48b11c", "seed_0001_case_48b11c", "evt_001", "ambience", "contextual ambience bed", "full_clip", false, new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0002", "artifacts/audio_candidates/week12_procedural_baseline_v0/0002_slot_v0_0002_generation_fallback.wav", "procedural_baseline_v0", "seed_0001_case_48b11c", "seed_0001_case_48b11c", "evt_002", "foley", "time-aligned footsteps", "event_local", true, new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0003", "artifacts/audio_candidates/week12_procedural_baseline_v0/0003_slot_v0_0003_generation_fallback.wav", "procedural_baseline_v0", "seed_0002_case_adc34a", "seed_0002_case_adc34a", "evt_001", "ambience", "contextual ambience bed", "full_clip", false, new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0004", "artifacts/audio_candidates/week12_procedural_baseline_v0/0004_slot_v0_0004_generation_fallback.wav", "procedural_baseline_v0", "seed_0002_case_adc34a", "seed_0002_case_adc34a", "evt_002", "foley", "door close impact", "event_local", true, new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0005", "artifacts/audio_candidates/week12_procedural_baseline_v0/0005_slot_v0_0005_generation_fallback.wav", "procedural_baseline_v0", "seed_0003_case_9eb26a", "seed_0003_case_9eb26a", "evt_001", "ambience", "contextual ambience bed", "full_clip", false, new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0006", "artifacts/audio_candidates/week12_procedural_baseline_v0/0006_slot_v0_0006_generation_fallback.wav", "procedural_baseline_v0", "seed_0003_case_9eb26a", "seed_0003_case_9eb26a", "evt_002", "foley", "raindrop impacts on window", "event_local", true, new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0007", "artifacts/audio_candidates/week12_procedural_baseline_v0/0007_slot_v0_0007_generation_fallback.wav", "procedural_baseline_v0", "seed_0004_case_f9f0fb", "seed_0004_case_f9f0fb", "evt_001", "ambience", "contextual ambience bed", "full_clip", false, new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0008", "artifacts/audio_candidates/week12_procedural_baseline_v0/0008_slot_v0_0008_generation_fallback.wav", "procedural_baseline_v0", "seed_0004_case_f9f0fb", "seed_0004_case_f9f0fb", "evt_002", "foley", "keyboard typing taps", "event_local", true, new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), new BigDecimal("5.76"), new BigDecimal("1.44"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0009", "artifacts/audio_candidates/week12_procedural_baseline_v0/0009_slot_v0_0009_generation_fallback.wav", "procedural_baseline_v0", "seed_0005_case_06f366", "seed_0005_case_06f366", "evt_001", "ambience", "outdoor bike-pass ambience", "full_clip", false, new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), new BigDecimal("8.0"), new BigDecimal("0.0"), "READY_FOR_RUNTIME_PLACEMENT"),
                new AudioArtifactRegistryItem("procedural_v0_0010", "artifacts/audio_candidates/week12_procedural_baseline_v0/0010_slot_v0_0010_generation_fallback.wav", "procedural_baseline_v0", "seed_0005_case_06f366", "seed_0005_case_06f366", "evt_002", "foley", "bicycle pass-by motion cue", "event_local", true, new BigDecimal("1.2"), new BigDecimal("5.44"), new BigDecimal("1.2"), new BigDecimal("5.44"), new BigDecimal("1.2"), "READY_FOR_RUNTIME_PLACEMENT")
    );

    public AudioArtifactRegistryResponse list() {
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
    }

    public Optional<AudioArtifactRegistryItem> findByCandidateId(String candidateId) {
        return items.stream()
                .filter(item -> item.candidateId().equals(candidateId))
                .findFirst();
    }
}
