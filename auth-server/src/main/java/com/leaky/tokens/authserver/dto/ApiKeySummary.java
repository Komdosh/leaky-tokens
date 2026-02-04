package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiKeySummary(
    @Schema(example = "11111111-1111-1111-1111-111111111111") UUID id,
    @Schema(example = "cli-key") String name,
    @Schema(example = "2026-02-04T17:00:00Z") Instant createdAt,
    @Schema(example = "2026-12-31T00:00:00Z") Instant expiresAt
) {
}
