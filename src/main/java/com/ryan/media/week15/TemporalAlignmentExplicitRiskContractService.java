package com.ryan.media.week15;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TemporalAlignmentExplicitRiskContractService {

    private static final String DEFAULT_CONTRACT_PATH =
            "artifacts/manifests/week15_mainbase_explicit_risk_contract.json";

    private final ObjectMapper objectMapper;

    public TemporalAlignmentExplicitRiskContractService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> readContract() throws IOException {
        Path path = Path.of(System.getProperty("week15.explicitRiskContract.path", DEFAULT_CONTRACT_PATH));
        JsonNode root = objectMapper.readTree(path.toFile());
        JsonNode summary = root.path("summary");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemaVersion", "week15.java.temporal_alignment.explicit_risk_contract.consumer.v1");
        response.put("sourceContractPath", path.toString());
        response.put("sourceSchemaVersion", root.path("schemaVersion").asText(""));
        response.put("decision", root.path("decision").asText(""));
        response.put("failures", toStringList(root.path("failures")));
        response.put("candidateTotal", summary.path("candidateTotal").asInt(0));
        response.put("actionableRiskCandidateIds", toStringList(summary.path("actionableRiskCandidateIds")));
        response.put("nonActionableCandidateIds", toStringList(summary.path("nonActionableCandidateIds")));
        response.put("alertEligibleCandidateIds", toStringList(summary.path("alertEligibleCandidateIds")));
        response.put("blockedClaims", toStringList(summary.path("blockedClaims")));
        response.put("candidateRisks", candidateRisks(root.path("candidateRiskRows")));
        response.put("gateDecision", buildGateDecision(root, summary));
        response.put("nextAction",
                "Cloud should consume this explicit Java API instead of inferring candidate risk from text.");
        return response;
    }

    private String buildGateDecision(JsonNode root, JsonNode summary) {
        boolean pass = "PASS".equals(root.path("decision").asText(""));
        List<String> actionable = toStringList(summary.path("actionableRiskCandidateIds"));
        List<String> alertEligible = toStringList(summary.path("alertEligibleCandidateIds"));
        if (pass && actionable.equals(alertEligible) && actionable.size() == 2) {
            return "PASS_EXPLICIT_RISK_CONTRACT_CONSUMER";
        }
        return "BLOCK_EXPLICIT_RISK_CONTRACT_CONSUMER";
    }

    private List<Map<String, Object>> candidateRisks(JsonNode rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!rows.isArray()) {
            return out;
        }
        for (JsonNode row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("candidateId", row.path("candidateId").asText(""));
            item.put("riskClass", row.path("riskClass").asText(""));
            item.put("actionability", row.path("actionability").asText(""));
            item.put("evidenceType", row.path("evidenceType").asText(""));
            item.put("alertEligible", row.path("alertEligible").asBoolean(false));
            item.put("requiresHumanReview", row.path("requiresHumanReview").asBoolean(false));
            item.put("sourceCount", row.path("sourceCount").asInt(0));
            item.put("reason", row.path("reason").asText(""));
            out.add(item);
        }
        return out;
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (!node.isArray()) {
            return out;
        }
        for (JsonNode item : node) {
            out.add(item.asText());
        }
        return out;
    }
}
