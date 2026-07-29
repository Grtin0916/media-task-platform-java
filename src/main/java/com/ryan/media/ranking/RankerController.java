package com.ryan.media.ranking;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rankers")
public class RankerController {
    private static final String DEFAULT_BUNDLE =
            "artifacts/models/preference_ranker_v1/delivery";
    private final RankerBundleImporter importer;
    private final RankerRegistry registry;

    public RankerController(RankerBundleImporter importer, RankerRegistry registry) {
        this.importer = importer;
        this.registry = registry;
    }

    @PostMapping("/import")
    public ResponseEntity<RankerRegistry.ImportResult> importBundle(
            @RequestBody(required = false) ImportRequest request) {
        String path = request == null || request.bundlePath() == null
                ? DEFAULT_BUNDLE : request.bundlePath();
        RankerRegistry.ImportResult result = importer.importBundle(path);
        return ResponseEntity.ok()
                .eTag("\"" + result.version().bundleDigest() + "\"")
                .body(result);
    }

    @GetMapping
    public List<RankerVersion> list() {
        return registry.list();
    }

    @GetMapping("/{version}")
    public ResponseEntity<?> version(
            @PathVariable String version,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        RankerVersion found = registry.require(version);
        String etag = "\"" + found.bundleDigest() + "\"";
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(found);
    }

    @GetMapping("/{version}/metrics")
    public Map<String, Object> metrics(@PathVariable String version) {
        RankerVersion found = registry.require(version);
        return Map.of(
                "rankerVersion", found.rankerVersion(),
                "promotionStatus", found.promotionStatus(),
                "modelPresent", found.modelPresent(),
                "oofAvailable", found.oofAvailable(),
                "recommendationCount", found.recommendationCount(),
                "reviewSubmittedCount", found.reviewSubmittedCount(),
                "finalSelectedMutationCount", found.finalSelectedMutationCount());
    }

    @GetMapping("/{version}/artifacts")
    public Map<String, Object> artifacts(@PathVariable String version) {
        RankerVersion found = registry.require(version);
        return Map.of(
                "rankerVersion", found.rankerVersion(),
                "bundleDigest", found.bundleDigest(),
                "storedBundlePath", found.storedBundlePath());
    }

    public record ImportRequest(String bundlePath) {
    }
}
