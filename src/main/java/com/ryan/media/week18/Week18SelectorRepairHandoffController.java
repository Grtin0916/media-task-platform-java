package com.ryan.media.week18;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/week18/selector-repair")
public class Week18SelectorRepairHandoffController {

    private static final String DEFAULT_CONTRACT =
            "artifacts/week18/w18_selector_repair_handoff_20260708.json";

    private final ObjectMapper objectMapper;

    public Week18SelectorRepairHandoffController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public JsonNode contract() {
        return loadContract();
    }

    @GetMapping("/summary")
    public ObjectNode summary() {
        JsonNode root = loadContract();
        ObjectNode out = objectMapper.createObjectNode();
        out.put("contract_name", root.path("contract_name").asText());
        out.put("contract_version", root.path("contract_version").asText());
        out.put("producer", root.path("producer").asText());
        out.set("intended_consumers", root.path("intended_consumers"));
        out.put("boundary", root.path("boundary").asText());
        out.set("summary", root.path("summary"));
        return out;
    }

    @GetMapping("/winners")
    public ArrayNode winners() {
        JsonNode root = loadContract();
        ArrayNode out = objectMapper.createArrayNode();
        root.path("winners").forEach(out::add);
        return out;
    }

    @GetMapping("/repairs")
    public ArrayNode repairedCandidates() {
        JsonNode root = loadContract();
        ArrayNode out = objectMapper.createArrayNode();
        root.path("repaired_candidates").forEach(out::add);
        return out;
    }

    @GetMapping("/cases/{caseId}")
    public ObjectNode caseView(@PathVariable String caseId) {
        JsonNode root = loadContract();
        ObjectNode out = objectMapper.createObjectNode();
        ArrayNode winners = objectMapper.createArrayNode();
        ArrayNode repairs = objectMapper.createArrayNode();

        root.path("winners").forEach(node -> {
            if (caseId.equals(node.path("case_id").asText())) {
                winners.add(node);
            }
        });

        root.path("repaired_candidates").forEach(node -> {
            if (caseId.equals(node.path("case_id").asText())) {
                repairs.add(node);
            }
        });

        out.put("case_id", caseId);
        out.set("winners", winners);
        out.set("repaired_candidates", repairs);
        out.put("winner_count", winners.size());
        out.put("repair_count", repairs.size());
        out.put("boundary", root.path("boundary").asText());
        return out;
    }

    private JsonNode loadContract() {
        String configured = System.getProperty("week18.selectorRepairHandoff", DEFAULT_CONTRACT);
        Path path = Paths.get(configured);
        if (!Files.exists(path)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Week18 selector repair handoff contract not found: " + path);
        }
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read Week18 selector repair handoff contract: " + path,
                    ex);
        }
    }
}
