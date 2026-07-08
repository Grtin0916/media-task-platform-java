package com.ryan.media.week18;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = Week18SelectorRepairHandoffControllerIT.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "week18.selectorRepairHandoff=artifacts/week18/w18_selector_repair_handoff_20260708.json"
        }
)
class Week18SelectorRepairHandoffControllerIT {

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void summaryReturnsMainbaseSelectorRepairContract() throws Exception {
        JsonNode root = getJson("/api/week18/selector-repair/summary");

        assertEquals("week18_selector_repair_handoff", root.path("contract_name").asText());
        assertEquals("2026-07-08.v1", root.path("contract_version").asText());
        assertEquals("mainbase", root.path("producer").asText());
        assertEquals(6, root.path("summary").path("winner_count").asInt());
        assertEquals(6, root.path("summary").path("repair_probe_count").asInt());
        assertEquals(0, root.path("summary").path("missing_asset_count").asInt());
        assertTrue(root.path("boundary").asText().contains("not a full repair engine"));
    }

    @Test
    void caseViewReturnsWinnerOrRepairEvidence() throws Exception {
        JsonNode root = getJson("/api/week18/selector-repair/cases/glass_drop_room_001");

        assertEquals("glass_drop_room_001", root.path("case_id").asText());
        assertTrue(root.path("winner_count").asInt() >= 1);
        assertTrue(root.path("repair_count").asInt() >= 1);
        assertTrue(root.path("boundary").asText().contains("proxy-backed"));
    }

    private JsonNode getJson(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();

        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode(), resp.body());
        return objectMapper.readTree(resp.body());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
            org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
            org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration.class
    })
    @Import(Week18SelectorRepairHandoffController.class)
    static class TestApp {
    }
}
