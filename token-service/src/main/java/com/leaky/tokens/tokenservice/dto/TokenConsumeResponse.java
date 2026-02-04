package com.leaky.tokens.tokenservice.dto;

import java.time.Instant;
import java.util.Map;

public class TokenConsumeResponse {
    private final boolean allowed;
    private final long capacity;
    private final long used;
    private final long remaining;
    private final long waitSeconds;
    private final Instant timestamp;
    private final Map<String, Object> providerResponse;

    public TokenConsumeResponse(boolean allowed, long capacity, long used, long remaining, long waitSeconds,
                                Instant timestamp, Map<String, Object> providerResponse) {
        this.allowed = allowed;
        this.capacity = capacity;
        this.used = used;
        this.remaining = remaining;
        this.waitSeconds = waitSeconds;
        this.timestamp = timestamp;
        this.providerResponse = providerResponse;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getUsed() {
        return used;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getWaitSeconds() {
        return waitSeconds;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getProviderResponse() {
        return providerResponse;
    }
}
