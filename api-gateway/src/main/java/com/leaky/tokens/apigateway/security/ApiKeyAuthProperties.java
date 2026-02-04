package com.leaky.tokens.apigateway.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "gateway.api-key")
public class ApiKeyAuthProperties {
    private boolean enabled = true;
    @NotBlank
    private String headerName = "X-Api-Key";
    @NotBlank
    private String authServerUrl = "http://localhost:8081";
    @NotBlank
    private String userHeaderName = "X-User-Id";
    @NotBlank
    private String rolesHeaderName = "X-User-Roles";
    @Min(1)
    private long cacheTtlSeconds = 120;
    @Min(1)
    private long cacheMaxSize = 10000;

}
