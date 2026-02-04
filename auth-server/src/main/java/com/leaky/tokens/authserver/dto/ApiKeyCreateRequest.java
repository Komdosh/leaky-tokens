package com.leaky.tokens.authserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ApiKeyCreateRequest {
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private String userId;
    @Schema(example = "cli-key")
    private String name;
    @Schema(example = "2026-12-31T00:00:00Z")
    private Instant expiresAt;

}
