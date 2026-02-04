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
}
