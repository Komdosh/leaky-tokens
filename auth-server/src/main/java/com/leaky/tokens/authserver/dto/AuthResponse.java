package com.leaky.tokens.authserver.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
    @Schema(example = "00000000-0000-0000-0000-000000000001") UUID userId,
    @Schema(example = "demo-user") String username,
    @Schema(example = "eyJhbGciOiJSUzI1NiJ9...") String token,
    @Schema(example = "[\"USER\"]") List<String> roles
) {
}
