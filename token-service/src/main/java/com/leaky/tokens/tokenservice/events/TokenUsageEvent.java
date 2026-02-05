package com.leaky.tokens.tokenservice.events;

import java.time.Instant;

public record TokenUsageEvent(String userId, String provider, long tokens, boolean allowed, Instant timestamp) {
}
