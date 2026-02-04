package com.leaky.tokens.tokenservice.dto;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenConsumeResponse {
    @Schema(example = "true")
    private final boolean allowed;
    @Schema(example = "1000")
    private final long capacity;
    @Schema(example = "100")
    private final long used;
    @Schema(example = "900")
    private final long remaining;
    @Schema(example = "0")
    private final long waitSeconds;
    @Schema(example = "2026-02-04T17:00:00Z")
    private final Instant timestamp;
    @Schema(example = "{\"message\":\"ok\"}", description = "Provider response payload (raw JSON map)")
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
