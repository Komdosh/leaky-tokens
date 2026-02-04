package com.leaky.tokens.tokenservice.bucket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TokenBucketEngineTest {
    @Test
    void tokenBucketRefillsAndConsumes() {
        TokenBucketEngine engine = new TokenBucketEngine();
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setStrategy(TokenBucketStrategy.TOKEN_BUCKET);
        properties.setCapacity(10);
        properties.setLeakRatePerSecond(1.0);

        TokenBucketState state = new TokenBucketState(0L, Instant.parse("2026-02-03T10:00:00Z"));
        Instant later = Instant.parse("2026-02-03T10:00:10Z");

        TokenBucketResult first = engine.tryConsume(state, properties, 5, later);
        assertThat(first.isAllowed()).isTrue();
        assertThat(first.getRemaining()).isEqualTo(5);

        TokenBucketResult second = engine.tryConsume(state, properties, 6, later);
        assertThat(second.isAllowed()).isFalse();
        assertThat(second.getWaitSeconds()).isGreaterThan(0);
    }

    @Test
    void fixedWindowResetsAfterWindow() {
        TokenBucketEngine engine = new TokenBucketEngine();
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setStrategy(TokenBucketStrategy.FIXED_WINDOW);
        properties.setCapacity(5);
        properties.setWindowSeconds(10);

        TokenBucketState state = new TokenBucketState(0L, null);
        Instant t0 = Instant.parse("2026-02-03T10:00:00Z");
        TokenBucketResult first = engine.tryConsume(state, properties, 5, t0);
        assertThat(first.isAllowed()).isTrue();

        Instant t1 = Instant.parse("2026-02-03T10:00:05Z");
        TokenBucketResult denied = engine.tryConsume(state, properties, 1, t1);
        assertThat(denied.isAllowed()).isFalse();
        assertThat(denied.getWaitSeconds()).isGreaterThan(0);

        Instant t2 = Instant.parse("2026-02-03T10:00:11Z");
        TokenBucketResult afterReset = engine.tryConsume(state, properties, 1, t2);
        assertThat(afterReset.isAllowed()).isTrue();
    }

    @Test
    void tokenBucketWaitsForRefill() {
        TokenBucketEngine engine = new TokenBucketEngine();
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setStrategy(TokenBucketStrategy.TOKEN_BUCKET);
        properties.setCapacity(3);
        properties.setLeakRatePerSecond(1.0);

        TokenBucketState state = new TokenBucketState(3L, Instant.parse("2026-02-03T10:00:00Z"));
        Instant t0 = Instant.parse("2026-02-03T10:00:00Z");
        TokenBucketResult consumeAll = engine.tryConsume(state, properties, 3, t0);
        assertThat(consumeAll.isAllowed()).isTrue();

        Instant t1 = Instant.parse("2026-02-03T10:00:01Z");
        TokenBucketResult denied = engine.tryConsume(state, properties, 2, t1);
        assertThat(denied.isAllowed()).isFalse();
        assertThat(denied.getWaitSeconds()).isGreaterThan(0);

        Instant t2 = Instant.parse("2026-02-03T10:00:03Z");
        TokenBucketResult allowed = engine.tryConsume(state, properties, 2, t2);
        assertThat(allowed.isAllowed()).isTrue();
    }

    @Test
    void leakyBucketWaitsIndefinitelyWhenLeakRateZero() {
        TokenBucketEngine engine = new TokenBucketEngine();
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setStrategy(TokenBucketStrategy.LEAKY_BUCKET);
        properties.setCapacity(10);
        properties.setLeakRatePerSecond(0.0);

        TokenBucketState state = new TokenBucketState(10L, Instant.parse("2026-02-03T10:00:00Z"));
        TokenBucketResult denied = engine.tryConsume(state, properties, 1, Instant.parse("2026-02-03T10:00:01Z"));

        assertThat(denied.isAllowed()).isFalse();
        assertThat(denied.getWaitSeconds()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void fixedWindowNormalizesZeroWindowSeconds() {
        TokenBucketEngine engine = new TokenBucketEngine();
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setStrategy(TokenBucketStrategy.FIXED_WINDOW);
        properties.setCapacity(2);
        properties.setWindowSeconds(0);

        TokenBucketState state = new TokenBucketState(0L, null);
        Instant t0 = Instant.parse("2026-02-03T10:00:00Z");
        TokenBucketResult first = engine.tryConsume(state, properties, 2, t0);
        assertThat(first.isAllowed()).isTrue();

        Instant t1 = Instant.parse("2026-02-03T10:00:00Z");
        TokenBucketResult denied = engine.tryConsume(state, properties, 1, t1);
        assertThat(denied.isAllowed()).isFalse();

        Instant t2 = Instant.parse("2026-02-03T10:00:01Z");
        TokenBucketResult afterReset = engine.tryConsume(state, properties, 1, t2);
        assertThat(afterReset.isAllowed()).isTrue();
    }

    @Test
    void tokenBucketRefillsOnFirstUse() {
        TokenBucketEngine engine = new TokenBucketEngine();
        TokenBucketProperties properties = new TokenBucketProperties();
        properties.setStrategy(TokenBucketStrategy.TOKEN_BUCKET);
        properties.setCapacity(5);
        properties.setLeakRatePerSecond(1.0);

        TokenBucketState state = new TokenBucketState(0L, null);
        TokenBucketResult result = engine.tryConsume(state, properties, 2, Instant.parse("2026-02-03T10:00:00Z"));

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getCapacity()).isEqualTo(5);
        assertThat(result.getUsed()).isEqualTo(2);
    }
}
