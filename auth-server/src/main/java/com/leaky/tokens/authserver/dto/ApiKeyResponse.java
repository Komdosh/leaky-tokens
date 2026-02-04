package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.UUID;

public class ApiKeyResponse {
    private final UUID id;
    private final UUID userId;
    private final String name;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final String apiKey;

    public ApiKeyResponse(UUID id,
                          UUID userId,
                          String name,
                          Instant createdAt,
                          Instant expiresAt,
                          String apiKey) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.apiKey = apiKey;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
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

    public String getApiKey() {
        return apiKey;
    }
}
