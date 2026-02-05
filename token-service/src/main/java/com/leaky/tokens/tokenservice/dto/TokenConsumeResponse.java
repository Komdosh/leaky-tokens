package com.leaky.tokens.tokenservice.dto;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenConsumeResponse(@Schema(example = "true") boolean allowed, @Schema(example = "1000") long capacity,
                                   @Schema(example = "100") long used, @Schema(example = "900") long remaining,
                                   @Schema(example = "0") long waitSeconds,
                                   @Schema(example = "2026-02-04T17:00:00Z") Instant timestamp,
                                   @Schema(example = "{\"message\":\"ok\"}", description = "Provider response payload (raw JSON map)") Map<String, Object> providerResponse) {
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

    @Override
    public boolean allowed() {
        return allowed;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public long used() {
        return used;
    }

    @Override
    public long remaining() {
        return remaining;
    }

    @Override
    public long waitSeconds() {
        return waitSeconds;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public Map<String, Object> providerResponse() {
        return providerResponse;
    }
}
