package com.ryan.media.week13;

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
