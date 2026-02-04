package com.leaky.tokens.authserver.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
    @Schema(example = "invalid credentials") String message,
    @Schema(example = "2026-02-04T17:00:00Z") Instant timestamp
) {
}
