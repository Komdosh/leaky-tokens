package com.leaky.tokens.apigateway.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {
    private boolean enabled = true;
    @Min(1)
    private long capacity = 120;
    @Min(1)
    private long windowSeconds = 60;
    @NotNull
    private RateLimitKeyStrategy keyStrategy = RateLimitKeyStrategy.IP;
    @NotBlank
    private String headerName = "X-Api-Key";
    @NotBlank
    private String userHeaderName = "X-User-Id";
    private java.time.Duration counterTtl = java.time.Duration.ofHours(6);
    private java.time.Duration cleanupInterval = java.time.Duration.ofMinutes(30);
    private java.util.List<String> whitelistPaths = java.util.List.of();
    @Valid
    private java.util.Map<String, RouteLimitConfig> routes = new java.util.HashMap<>();

    @Setter
    @Getter
    public static class RouteLimitConfig {
        @Min(1)
        private Long capacity;
        @Min(1)
        private Long windowSeconds;

    }
}
