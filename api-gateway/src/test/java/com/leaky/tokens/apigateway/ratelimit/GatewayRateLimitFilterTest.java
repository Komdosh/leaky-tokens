package com.leaky.tokens.apigateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import com.leaky.tokens.apigateway.flags.GatewayFeatureFlags;
import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class GatewayRateLimitFilterTest {
    @Test
    void cleanupRemovesExpiredCounters() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setCounterTtl(Duration.ofSeconds(1));
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
            properties,
            new GatewayMetrics(new SimpleMeterRegistry()),
            featureFlags()
        );

        ConcurrentHashMap<String, Object> counters = counters(filter);
        Object counter = newWindowCounter(Instant.now().minusSeconds(5), 0L);
        counters.put("key-1", counter);

        filter.cleanup();

        assertThat(counters).isEmpty();
    }

    @Test
    void cleanupSkipsWhenTtlZero() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setCounterTtl(Duration.ZERO);
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
            properties,
            new GatewayMetrics(new SimpleMeterRegistry()),
            featureFlags()
        );

        ConcurrentHashMap<String, Object> counters = counters(filter);
        Object counter = newWindowCounter(Instant.now().minusSeconds(5), 0L);
        counters.put("key-1", counter);

        filter.cleanup();

        assertThat(counters).containsKey("key-1");
    }

    @Test
    void cleanupKeepsRecentCounters() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setCounterTtl(Duration.ofSeconds(10));
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
            properties,
            new GatewayMetrics(new SimpleMeterRegistry()),
            featureFlags()
        );

        ConcurrentHashMap<String, Object> counters = counters(filter);
        Object counter = newWindowCounter(Instant.now(), 0L);
        counters.put("key-1", counter);

        filter.cleanup();

        assertThat(counters).containsKey("key-1");
    }

    @Test
    void filterBypassesWhenFeatureFlagDisabled() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        GatewayFeatureFlags flags = new GatewayFeatureFlags();
        flags.setRateLimiting(false);
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
            properties,
            new GatewayMetrics(new SimpleMeterRegistry()),
            flags
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/test").build()
        );
        GatewayFilterChain chain = ex -> ex.getResponse().setComplete();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isNull();
        assertThat(counters(filter)).isEmpty();
    }

    @Test
    void filterSkipsWhitelistedPath() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setWhitelistPaths(java.util.List.of("/actuator/**"));
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
            properties,
            new GatewayMetrics(new SimpleMeterRegistry()),
            featureFlags()
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/actuator/health").build()
        );
        GatewayFilterChain chain = ex -> ex.getResponse().setComplete();

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isNull();
        assertThat(counters(filter)).isEmpty();
    }

    @Test
    void filterUsesUserHeaderWhenConfigured() throws Exception {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setKeyStrategy(RateLimitKeyStrategy.USER_HEADER);
        GatewayRateLimitFilter filter = new GatewayRateLimitFilter(
            properties,
            new GatewayMetrics(new SimpleMeterRegistry()),
            featureFlags()
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/test")
                .header("X-User-Id", "user-9")
                .build()
        );
        GatewayFilterChain chain = ex -> ex.getResponse().setComplete();

        filter.filter(exchange, chain).block();

        assertThat(counters(filter).keySet()).contains("user:user-9");
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Object> counters(GatewayRateLimitFilter filter) throws Exception {
        Field field = GatewayRateLimitFilter.class.getDeclaredField("counters");
        field.setAccessible(true);
        return (ConcurrentHashMap<String, Object>) field.get(filter);
    }

    private Object newWindowCounter(Instant start, long count) throws Exception {
        Class<?> counterClass = Class.forName("com.leaky.tokens.apigateway.ratelimit.GatewayRateLimitFilter$WindowCounter");
        Constructor<?> ctor = counterClass.getDeclaredConstructor(Instant.class, long.class);
        ctor.setAccessible(true);
        return ctor.newInstance(start, count);
    }

    private GatewayFeatureFlags featureFlags() {
        GatewayFeatureFlags flags = new GatewayFeatureFlags();
        flags.setRateLimiting(true);
        return flags;
    }
}
