package com.leaky.tokens.tokenservice.events;

import java.time.Instant;

import org.springframework.stereotype.Component;

@Component
public class TokenUsageEventFactory {
    public TokenUsageEvent build(String userId, String provider, long tokens, boolean allowed, Instant timestamp) {
        return new TokenUsageEvent(userId, provider, tokens, allowed, timestamp);
    }
}
