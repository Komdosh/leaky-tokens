package com.leaky.tokens.tokenservice.bucket;

import java.time.Duration;
import java.time.Instant;

public class TokenBucketEngine {
    public TokenBucketResult tryConsume(TokenBucketState state,
                                        TokenBucketProperties properties,
                                        long tokens,
                                        Instant now) {
        TokenBucketStrategy strategy = properties.getStrategy();
        if (strategy == TokenBucketStrategy.FIXED_WINDOW) {
            return tryFixedWindow(state, properties.getCapacity(), properties.getWindowSeconds(), tokens, now);
        }
        if (strategy == TokenBucketStrategy.TOKEN_BUCKET) {
            return tryTokenBucket(state, properties.getCapacity(), properties.getLeakRatePerSecond(), tokens, now);
        }
        return tryLeakyBucket(state, properties.getCapacity(), properties.getLeakRatePerSecond(), tokens, now);
    }

    public TokenBucketResult tryConsume(TokenBucketState state, long capacity, double leakRatePerSecond,
                                        long tokens, Instant now) {
        return tryLeakyBucket(state, capacity, leakRatePerSecond, tokens, now);
    }

    private TokenBucketResult tryLeakyBucket(TokenBucketState state, long capacity, double leakRatePerSecond,
                                             long tokens, Instant now) {
        leak(state, leakRatePerSecond, now);
        long available = capacity - state.getCurrentTokens();
        if (tokens <= available) {
            state.setCurrentTokens(state.getCurrentTokens() + tokens);
            return TokenBucketResult.allowed(capacity, state.getCurrentTokens(), 0L, now);
        }
        long overflow = tokens - available;
        long waitSeconds = estimateWaitSeconds(overflow, leakRatePerSecond);
        return TokenBucketResult.denied(capacity, state.getCurrentTokens(), waitSeconds, now);
    }

    private TokenBucketResult tryFixedWindow(TokenBucketState state, long capacity, long windowSeconds,
                                             long tokens, Instant now) {
        long normalizedWindowSeconds = Math.max(1L, windowSeconds);
        Instant windowStart = state.getWindowStart();
        if (windowStart == null) {
            windowStart = now;
            state.setWindowStart(windowStart);
            state.setWindowCount(0L);
        }
        Instant windowEnd = windowStart.plusSeconds(normalizedWindowSeconds);
        if (!now.isBefore(windowEnd)) {
            windowStart = now;
            state.setWindowStart(windowStart);
            state.setWindowCount(0L);
            windowEnd = windowStart.plusSeconds(normalizedWindowSeconds);
        }

        long used = state.getWindowCount();
        if (used + tokens <= capacity) {
            used += tokens;
            state.setWindowCount(used);
            return TokenBucketResult.allowed(capacity, used, 0L, now);
        }

        long waitSeconds = Math.max(0L, Duration.between(now, windowEnd).getSeconds());
        return TokenBucketResult.denied(capacity, used, waitSeconds, now);
    }

    private TokenBucketResult tryTokenBucket(TokenBucketState state, long capacity, double refillRatePerSecond,
                                             long tokens, Instant now) {
        refill(state, capacity, refillRatePerSecond, now);
        long available = state.getCurrentTokens();
        if (tokens <= available) {
            long remaining = available - tokens;
            state.setCurrentTokens(remaining);
            return TokenBucketResult.allowed(capacity, capacity - remaining, 0L, now);
        }
        long deficit = tokens - available;
        long waitSeconds = estimateWaitSeconds(deficit, refillRatePerSecond);
        return TokenBucketResult.denied(capacity, capacity - available, waitSeconds, now);
    }

    private void refill(TokenBucketState state, long capacity, double refillRatePerSecond, Instant now) {
        Instant lastUpdated = state.getLastUpdated();
        if (lastUpdated == null) {
            state.setCurrentTokens(capacity);
            state.setLastUpdated(now);
            return;
        }
        if (now.isBefore(lastUpdated)) {
            state.setLastUpdated(now);
            return;
        }
        Duration elapsed = Duration.between(lastUpdated, now);
        double refill = elapsed.toMillis() / 1000.0 * refillRatePerSecond;
        if (refill <= 0.0) {
            return;
        }
        long refillTokens = (long) Math.floor(refill);
        if (refillTokens > 0) {
            long next = Math.min(capacity, state.getCurrentTokens() + refillTokens);
            state.setCurrentTokens(next);
            state.setLastUpdated(now);
        }
    }

    private void leak(TokenBucketState state, double leakRatePerSecond, Instant now) {
        Instant lastUpdated = state.getLastUpdated();
        if (lastUpdated == null) {
            state.setLastUpdated(now);
            return;
        }
        if (now.isBefore(lastUpdated)) {
            state.setLastUpdated(now);
            return;
        }
        Duration elapsed = Duration.between(lastUpdated, now);
        double leaked = elapsed.toMillis() / 1000.0 * leakRatePerSecond;
        if (leaked <= 0.0) {
            return;
        }
        long leakedTokens = (long) Math.floor(leaked);
        if (leakedTokens > 0) {
            state.setCurrentTokens(Math.max(0L, state.getCurrentTokens() - leakedTokens));
            state.setLastUpdated(now);
        }
    }

    private long estimateWaitSeconds(long overflow, double leakRatePerSecond) {
        if (leakRatePerSecond <= 0.0) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(overflow / leakRatePerSecond);
    }
}
