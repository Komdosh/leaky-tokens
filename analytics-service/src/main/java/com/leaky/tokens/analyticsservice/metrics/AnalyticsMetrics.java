package com.leaky.tokens.analyticsservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMetrics {
    private final MeterRegistry registry;

    public AnalyticsMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void usageQuery(String provider, String outcome) {
        registry.counter("analytics.usage.query.total", "provider", provider, "outcome", outcome).increment();
    }

    public void reportQuery(String provider, String outcome) {
        registry.counter("analytics.report.query.total", "provider", provider, "outcome", outcome).increment();
    }

    public void anomalyQuery(String provider, String outcome) {
        registry.counter("analytics.anomaly.query.total", "provider", provider, "outcome", outcome).increment();
    }
}
