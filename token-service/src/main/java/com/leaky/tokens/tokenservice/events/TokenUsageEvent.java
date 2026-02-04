package com.leaky.tokens.tokenservice.events;

import java.time.Instant;

public class TokenUsageEvent {
    private final String userId;
    private final String provider;
    private final long tokens;
    private final boolean allowed;
    private final Instant timestamp;

    public TokenUsageEvent(String userId, String provider, long tokens, boolean allowed, Instant timestamp) {
        this.userId = userId;
        this.provider = provider;
        this.tokens = tokens;
        this.allowed = allowed;
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public long getTokens() {
        return tokens;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
