package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiKeyResponse(
    @Schema(example = "11111111-1111-1111-1111-111111111111") UUID id,
    @Schema(example = "00000000-0000-0000-0000-000000000001") UUID userId,
    @Schema(example = "cli-key") String name,
    @Schema(example = "2026-02-04T17:00:00Z") Instant createdAt,
    @Schema(example = "2026-12-31T00:00:00Z") Instant expiresAt,
    @Schema(example = "leaky_...") String apiKey
) {
}
