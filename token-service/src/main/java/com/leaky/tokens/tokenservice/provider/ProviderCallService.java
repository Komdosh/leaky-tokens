package com.leaky.tokens.tokenservice.provider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ProviderCallService {
    private final ProviderClient providerClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;

    public ProviderCallService(ProviderClient providerClient,
                               CircuitBreakerRegistry circuitBreakerRegistry,
                               RetryRegistry retryRegistry,
                               BulkheadRegistry bulkheadRegistry,
                               TimeLimiterRegistry timeLimiterRegistry) {
        this.providerClient = providerClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("providerCalls");
        this.retry = retryRegistry.retry("providerCalls");
        this.bulkhead = bulkheadRegistry.bulkhead("providerCalls");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("providerCalls");
    }

    public ProviderResponse call(String provider, ProviderRequest request) {
        Supplier<ProviderResponse> supplier = () -> providerClient.call(provider, request);
        Supplier<ProviderResponse> decorated = Bulkhead.decorateSupplier(
            bulkhead,
            Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, supplier))
        );
        Supplier<CompletableFuture<ProviderResponse>> futureSupplier =
            () -> CompletableFuture.supplyAsync(decorated);
        try {
            return TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier).call();
        } catch (Exception ex) {
            throw new ProviderCallException("Provider call failed", ex);
        }
    }
}
