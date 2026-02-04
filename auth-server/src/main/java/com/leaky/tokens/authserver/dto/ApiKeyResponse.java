package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeyResponse(UUID id, UUID userId, String name, Instant createdAt, Instant expiresAt, String apiKey) {
}
