package com.leaky.tokens.apigateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.api-key")
public class ApiKeyAuthProperties {
    private boolean enabled = true;
    private String headerName = "X-Api-Key";
    private String authServerUrl = "http://localhost:8081";
    private String userHeaderName = "X-User-Id";
    private String rolesHeaderName = "X-User-Roles";
    private long cacheTtlSeconds = 120;
    private long cacheMaxSize = 10000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getUserHeaderName() {
        return userHeaderName;
    }

    public void setUserHeaderName(String userHeaderName) {
        this.userHeaderName = userHeaderName;
    }

    public String getRolesHeaderName() {
        return rolesHeaderName;
    }

    public void setRolesHeaderName(String rolesHeaderName) {
        this.rolesHeaderName = rolesHeaderName;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }
}
