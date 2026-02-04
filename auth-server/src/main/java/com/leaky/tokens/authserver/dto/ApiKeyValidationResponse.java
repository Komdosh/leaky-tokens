package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiKeyValidationResponse(UUID userId, String name, Instant expiresAt, List<String> roles) {
}
