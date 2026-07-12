package com.ryan.media.week18.lifecycle;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class W18TaskLifecycleMetricsConfiguration {

    private static final String METRIC_NAME =
            "media.week18.lifecycle.snapshot";

    @Bean
    public MeterBinder w18TaskLifecycleMetrics(
            W18TaskLifecycleService lifecycleService
    ) {
        return registry -> {
            registerGauge(
                    registry,
                    lifecycleService,
                    "task_total",
                    "taskCount"
            );
            registerGauge(
                    registry,
                    lifecycleService,
                    "winner_succeeded",
                    "succeededCount"
            );
            registerGauge(
                    registry,
                    lifecycleService,
                    "repair_required",
                    "repairRequiredCount"
            );
            registerGauge(
                    registry,
                    lifecycleService,
                    "repair_applied",
                    "repairAppliedCount"
            );
            registerGauge(
                    registry,
                    lifecycleService,
                    "result_bound",
                    "resultBoundCount"
            );
            registerGauge(
                    registry,
                    lifecycleService,
                    "missing_asset",
                    "missingAssetCount"
            );
        };
    }

    private static void registerGauge(
            MeterRegistry registry,
            W18TaskLifecycleService lifecycleService,
            String category,
            String reportField
    ) {
        Gauge.builder(
                        METRIC_NAME,
                        lifecycleService,
                        service -> service
                                .lifecycleReport()
                                .path(reportField)
                                .asDouble()
                )
                .description(
                        "Current Week18 artifact-backed "
                                + "task lifecycle state"
                )
                .tag("category", category)
                .register(registry);
    }
}