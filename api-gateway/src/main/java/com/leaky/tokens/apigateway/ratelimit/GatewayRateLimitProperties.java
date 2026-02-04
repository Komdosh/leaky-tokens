package com.leaky.tokens.apigateway.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public RateLimitKeyStrategy getKeyStrategy() {
        return keyStrategy;
    }

    public void setKeyStrategy(RateLimitKeyStrategy keyStrategy) {
        this.keyStrategy = keyStrategy;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getUserHeaderName() {
        return userHeaderName;
    }

    public void setUserHeaderName(String userHeaderName) {
        this.userHeaderName = userHeaderName;
    }

    public java.time.Duration getCounterTtl() {
        return counterTtl;
    }

    public void setCounterTtl(java.time.Duration counterTtl) {
        this.counterTtl = counterTtl;
    }

    public java.time.Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(java.time.Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public java.util.List<String> getWhitelistPaths() {
        return whitelistPaths;
    }

    public void setWhitelistPaths(java.util.List<String> whitelistPaths) {
        this.whitelistPaths = whitelistPaths;
    }

    public java.util.Map<String, RouteLimitConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(java.util.Map<String, RouteLimitConfig> routes) {
        this.routes = routes;
    }

    public static class RouteLimitConfig {
        @Min(1)
        private Long capacity;
        @Min(1)
        private Long windowSeconds;

        public Long getCapacity() {
            return capacity;
        }

        public void setCapacity(Long capacity) {
            this.capacity = capacity;
        }

        public Long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(Long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
