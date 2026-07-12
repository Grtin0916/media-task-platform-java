package com.ryan.media.week18.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        classes = W18TaskLifecycleTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "w18.lifecycle.contract-path="
                        + "artifacts/week18/"
                        + "w18_selector_repair_handoff_20260708.json",
                "w18.lifecycle.test-context=metrics",
                "management.endpoints.web.exposure.include="
                        + "health,prometheus",
                "management.prometheus.metrics.export.enabled=true",
                "management.endpoint.prometheus.access=unrestricted"
        }
)
class W18TaskLifecycleMetricsHttpIT {

    private static final String BOOTSTRAP_PATH =
            "/api/week18/lifecycle/bootstrap";

    private static final String PROMETHEUS_PATH =
            "/actuator/prometheus";

    private static final String METRIC_NAME =
            "media_week18_lifecycle_snapshot";

    private static final Path OUTPUT_PATH = Path.of(
            "artifacts/metrics/"
                    + "week18_task_lifecycle_live_20260712.prom"
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesLiveLifecycleMetricsAfterBatchBootstrap()
            throws Exception {
        ResponseEntity<JsonNode> bootstrapResponse =
                restTemplate.postForEntity(
                        BOOTSTRAP_PATH,
                        Map.of(),
                        JsonNode.class
                );

        assertEquals(
                HttpStatus.OK.value(),
                bootstrapResponse.getStatusCode().value()
        );

        JsonNode bootstrapBody = bootstrapResponse.getBody();
        assertNotNull(bootstrapBody);
        assertEquals(
                12,
                bootstrapBody.path("taskCount").asInt()
        );
        assertEquals(
                12,
                bootstrapBody.path("resultBoundCount").asInt()
        );

        ResponseEntity<String> metricsResponse =
                restTemplate.getForEntity(
                        PROMETHEUS_PATH,
                        String.class
                );

        assertEquals(
                HttpStatus.OK.value(),
                metricsResponse.getStatusCode().value()
        );

        String metricsBody = metricsResponse.getBody();
        assertNotNull(metricsBody);

        assertMetric(
                metricsBody,
                "task_total",
                12.0
        );
        assertMetric(
                metricsBody,
                "winner_succeeded",
                6.0
        );
        assertMetric(
                metricsBody,
                "repair_required",
                6.0
        );
        assertMetric(
                metricsBody,
                "repair_applied",
                6.0
        );
        assertMetric(
                metricsBody,
                "result_bound",
                12.0
        );
        assertMetric(
                metricsBody,
                "missing_asset",
                0.0
        );

        String customMetrics = metricsBody
                .lines()
                .filter(
                        line -> line.startsWith(
                                "# HELP " + METRIC_NAME
                        )
                                || line.startsWith(
                                "# TYPE " + METRIC_NAME
                        )
                                || line.startsWith(
                                METRIC_NAME
                        )
                )
                .collect(
                        Collectors.joining(
                                System.lineSeparator()
                        )
                )
                + System.lineSeparator();

        Files.createDirectories(OUTPUT_PATH.getParent());
        Files.writeString(
                OUTPUT_PATH,
                customMetrics,
                StandardCharsets.UTF_8
        );

        assertTrue(Files.isRegularFile(OUTPUT_PATH));
        assertTrue(Files.size(OUTPUT_PATH) > 0);
    }

    private static void assertMetric(
            String metricsBody,
            String category,
            double expected
    ) {
        Pattern pattern = Pattern.compile(
                "(?m)^"
                        + Pattern.quote(METRIC_NAME)
                        + "\\{category=\""
                        + Pattern.quote(category)
                        + "\"} "
                        + "([-+]?[0-9]+(?:\\.[0-9]+)?)$"
        );

        Matcher matcher = pattern.matcher(metricsBody);

        assertTrue(
                matcher.find(),
                "Metric category not found: " + category
        );

        double actual = Double.parseDouble(
                matcher.group(1)
        );

        assertEquals(
                expected,
                actual,
                0.000001,
                category
        );
    }
}