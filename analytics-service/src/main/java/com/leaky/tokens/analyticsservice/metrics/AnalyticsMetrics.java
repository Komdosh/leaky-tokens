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
}
