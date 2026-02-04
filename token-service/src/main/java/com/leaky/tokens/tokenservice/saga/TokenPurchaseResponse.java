package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public class TokenPurchaseResponse {
    @Schema(example = "22222222-2222-2222-2222-222222222222")
    private final UUID sagaId;
    @Schema(example = "STARTED")
    private final TokenPurchaseSagaStatus status;
    @Schema(example = "2026-02-04T17:00:00Z")
    private final Instant createdAt;

    public TokenPurchaseResponse(UUID sagaId, TokenPurchaseSagaStatus status, Instant createdAt) {
        this.sagaId = sagaId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getSagaId() {
        return sagaId;
    }

    public TokenPurchaseSagaStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
