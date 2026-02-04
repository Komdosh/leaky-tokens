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
