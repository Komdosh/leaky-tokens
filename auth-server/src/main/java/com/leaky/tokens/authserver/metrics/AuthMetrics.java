package com.leaky.tokens.authserver.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {
    private final MeterRegistry registry;

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void registerSuccess() {
        registry.counter("auth.register.total", "outcome", "success").increment();
    }

    public void registerFailure(String reason) {
        registry.counter("auth.register.total", "outcome", "failure", "reason", reason).increment();
    }

    public void loginSuccess() {
        registry.counter("auth.login.total", "outcome", "success").increment();
    }

    public void loginFailure(String reason) {
        registry.counter("auth.login.total", "outcome", "failure", "reason", reason).increment();
    }

    public void apiKeyCreateSuccess() {
        registry.counter("auth.api_key.create.total", "outcome", "success").increment();
    }

    public void apiKeyCreateFailure(String reason) {
        registry.counter("auth.api_key.create.total", "outcome", "failure", "reason", reason).increment();
    }

    public void apiKeyValidateSuccess() {
        registry.counter("auth.api_key.validate.total", "outcome", "success").increment();
    }

    public void apiKeyValidateFailure(String reason) {
        registry.counter("auth.api_key.validate.total", "outcome", "failure", "reason", reason).increment();
    }
}
