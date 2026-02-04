package com.leaky.tokens.tokenservice.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;

class ProviderCallServiceTest {
    @Test
    void delegatesCall() {
        ProviderClient client = (provider, request) -> new ProviderResponse(provider, java.util.Map.of("ok", true));
        ProviderCallService service = new ProviderCallService(client,
            CircuitBreakerRegistry.ofDefaults(),
            RetryRegistry.ofDefaults(),
            BulkheadRegistry.ofDefaults(),
            TimeLimiterRegistry.ofDefaults());

        ProviderResponse response = service.call("openai", new ProviderRequest("hi"));
        assertThat(response.getProvider()).isEqualTo("openai");
        assertThat(response.getData()).containsEntry("ok", true);
    }

    @Test
    void wrapsExceptions() {
        ProviderClient client = (provider, request) -> { throw new RuntimeException("boom"); };
        ProviderCallService service = new ProviderCallService(client,
            CircuitBreakerRegistry.ofDefaults(),
            RetryRegistry.ofDefaults(),
            BulkheadRegistry.ofDefaults(),
            TimeLimiterRegistry.ofDefaults());

        assertThatThrownBy(() -> service.call("openai", new ProviderRequest("hi")))
            .isInstanceOf(ProviderCallException.class);
    }
}
