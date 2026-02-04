package com.leaky.tokens.tokenservice.bucket;

import java.time.Duration;
import java.time.Instant;

public class TokenBucket {
    private final long capacity;
    private final double leakRatePerSecond;
    private long currentTokens;
    private Instant lastUpdated;

    public TokenBucket(long capacity, double leakRatePerSecond, Instant now) {
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
        this.currentTokens = 0L;
        this.lastUpdated = now;
    }

    public synchronized TokenBucketResult tryConsume(long tokens, Instant now) {
        leak(now);
        long available = capacity - currentTokens;
        if (tokens <= available) {
            currentTokens += tokens;
            return TokenBucketResult.allowed(capacity, currentTokens, 0L, now);
        }
        long overflow = tokens - available;
        long waitSeconds = estimateWaitSeconds(overflow);
        return TokenBucketResult.denied(capacity, currentTokens, waitSeconds, now);
    }

    private void leak(Instant now) {
        if (now.isBefore(lastUpdated)) {
            lastUpdated = now;
            return;
        }
        Duration elapsed = Duration.between(lastUpdated, now);
        double leaked = elapsed.toMillis() / 1000.0 * leakRatePerSecond;
        if (leaked <= 0.0) {
            return;
        }
        long leakedTokens = (long) Math.floor(leaked);
        if (leakedTokens > 0) {
            currentTokens = Math.max(0L, currentTokens - leakedTokens);
            lastUpdated = now;
        }
    }

    private long estimateWaitSeconds(long overflow) {
        if (leakRatePerSecond <= 0.0) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(overflow / leakRatePerSecond);
    }
}
