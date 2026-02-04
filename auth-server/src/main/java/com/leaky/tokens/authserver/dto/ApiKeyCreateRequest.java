package com.leaky.tokens.authserver.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ApiKeyCreateRequest {
    private String userId;
    private String name;
    private Instant expiresAt;

}
