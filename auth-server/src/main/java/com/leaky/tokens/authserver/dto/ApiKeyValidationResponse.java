package com.leaky.tokens.authserver.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiKeyValidationResponse(
    @Schema(example = "00000000-0000-0000-0000-000000000001") UUID userId,
    @Schema(example = "cli-key") String name,
    @Schema(example = "2026-12-31T00:00:00Z") Instant expiresAt,
    @Schema(example = "[\"ADMIN\",\"USER\"]") List<String> roles
) {
}
