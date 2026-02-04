package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;
import java.util.UUID;

public class TokenPurchaseResponse {
    private final UUID sagaId;
    private final TokenPurchaseSagaStatus status;
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
