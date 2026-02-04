package com.leaky.tokens.tokenservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TokenServiceMetrics {
    private final MeterRegistry registry;

    public TokenServiceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void consumeAttempt(String provider) {
        registry.counter("token.consume.total", "provider", provider, "outcome", "attempt").increment();
    }

    public void consumeAllowed(String provider) {
        registry.counter("token.consume.total", "provider", provider, "outcome", "allowed").increment();
    }

    public void consumeRateLimited(String provider) {
        registry.counter("token.consume.total", "provider", provider, "outcome", "rate_limited").increment();
    }

    public void consumeQuotaInsufficient(String provider) {
        registry.counter("token.consume.total", "provider", provider, "outcome", "quota_insufficient").increment();
    }

    public void consumeProviderFailure(String provider) {
        registry.counter("token.consume.total", "provider", provider, "outcome", "provider_failure").increment();
    }

    public void quotaLookup(String provider, String outcome) {
        registry.counter("token.quota.lookup.total", "provider", provider, "outcome", outcome).increment();
    }
}
