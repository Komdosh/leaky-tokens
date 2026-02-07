package com.leaky.tokens.authserver.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class AuthMetricsTest {
    @Test
    void registerMetricsIncrementCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthMetrics metrics = new AuthMetrics(registry);

        metrics.registerSuccess();
        metrics.registerFailure("duplicate");

        assertThat(registry.counter("auth.register.total", "outcome", "success").count()).isEqualTo(1.0);
        assertThat(registry.counter("auth.register.total", "outcome", "failure", "reason", "duplicate").count())
            .isEqualTo(1.0);
    }

    @Test
    void loginMetricsIncrementCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthMetrics metrics = new AuthMetrics(registry);

        metrics.loginSuccess();
        metrics.loginFailure("bad-credentials");
        metrics.loginFailure("bad-credentials");

        assertThat(registry.counter("auth.login.total", "outcome", "success").count()).isEqualTo(1.0);
        assertThat(registry.counter("auth.login.total", "outcome", "failure", "reason", "bad-credentials").count())
            .isEqualTo(2.0);
    }

    @Test
    void apiKeyMetricsIncrementCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthMetrics metrics = new AuthMetrics(registry);

        metrics.apiKeyCreateSuccess();
        metrics.apiKeyCreateFailure("validation");
        metrics.apiKeyValidateSuccess();
        metrics.apiKeyValidateFailure("expired");

        assertThat(registry.counter("auth.api_key.create.total", "outcome", "success").count()).isEqualTo(1.0);
        assertThat(registry.counter("auth.api_key.create.total", "outcome", "failure", "reason", "validation").count())
            .isEqualTo(1.0);
        assertThat(registry.counter("auth.api_key.validate.total", "outcome", "success").count()).isEqualTo(1.0);
        assertThat(registry.counter("auth.api_key.validate.total", "outcome", "failure", "reason", "expired").count())
            .isEqualTo(1.0);
    }
}
