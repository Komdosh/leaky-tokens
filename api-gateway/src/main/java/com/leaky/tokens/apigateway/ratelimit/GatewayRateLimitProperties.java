package com.leaky.tokens.apigateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.rate-limit")
public class GatewayRateLimitProperties {
    private boolean enabled = true;
    private long capacity = 120;
    private long windowSeconds = 60;
    private RateLimitKeyStrategy keyStrategy = RateLimitKeyStrategy.IP;
    private String headerName = "X-Api-Key";
    private String userHeaderName = "X-User-Id";
    private java.time.Duration counterTtl = java.time.Duration.ofHours(6);
    private java.time.Duration cleanupInterval = java.time.Duration.ofMinutes(30);
    private java.util.List<String> whitelistPaths = java.util.List.of();
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
        private Long capacity;
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
