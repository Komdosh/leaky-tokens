package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.UUID;

public record ApiKeySummary(UUID id, String name, Instant createdAt, Instant expiresAt) {
}
