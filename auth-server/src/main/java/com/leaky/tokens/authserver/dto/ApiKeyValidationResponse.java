package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ApiKeyValidationResponse {
    private final UUID userId;
    private final String name;
    private final Instant expiresAt;
    private final List<String> roles;

    public ApiKeyValidationResponse(UUID userId, String name, Instant expiresAt, List<String> roles) {
        this.userId = userId;
        this.name = name;
        this.expiresAt = expiresAt;
        this.roles = roles;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<String> getRoles() {
        return roles;
    }
}
