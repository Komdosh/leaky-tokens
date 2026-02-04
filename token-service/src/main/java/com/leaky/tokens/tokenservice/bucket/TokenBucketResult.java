package com.leaky.tokens.tokenservice.bucket;

import lombok.Getter;

import java.time.Instant;

@Getter
public class TokenBucketResult {
    private final boolean allowed;
    private final long capacity;
    private final long used;
    private final long remaining;
    private final long waitSeconds;
    private final Instant timestamp;

    private TokenBucketResult(boolean allowed, long capacity, long used, long remaining, long waitSeconds, Instant timestamp) {
        this.allowed = allowed;
        this.capacity = capacity;
        this.used = used;
        this.remaining = remaining;
        this.waitSeconds = waitSeconds;
        this.timestamp = timestamp;
    }

    public static TokenBucketResult allowed(long capacity, long used, long waitSeconds, Instant timestamp) {
        return new TokenBucketResult(true, capacity, used, capacity - used, waitSeconds, timestamp);
    }

    public static TokenBucketResult denied(long capacity, long used, long waitSeconds, Instant timestamp) {
        return new TokenBucketResult(false, capacity, used, capacity - used, waitSeconds, timestamp);
    }

}
