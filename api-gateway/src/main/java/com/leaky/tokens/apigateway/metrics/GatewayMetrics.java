package com.leaky.tokens.apigateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GatewayMetrics {
    private final MeterRegistry registry;

    public GatewayMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void apiKeyValidation(String outcome) {
        registry.counter("gateway.api_key.validation.total", "outcome", outcome).increment();
    }

    public void apiKeyCache(String outcome) {
        registry.counter("gateway.api_key.cache.total", "outcome", outcome).increment();
    }

    public void rateLimit(String outcome) {
        registry.counter("gateway.rate_limit.total", "outcome", outcome).increment();
    }
}
