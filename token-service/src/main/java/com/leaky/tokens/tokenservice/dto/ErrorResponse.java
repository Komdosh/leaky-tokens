package com.leaky.tokens.tokenservice.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
    @Schema(example = "provider is required") String message,
    @Schema(example = "2026-02-04T17:00:00Z") Instant timestamp
) {
}
