package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.UUID;

public class ApiKeySummary {
    private final UUID id;
    private final String name;
    private final Instant createdAt;
    private final Instant expiresAt;

    public ApiKeySummary(UUID id, String name, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
