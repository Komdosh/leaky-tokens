package com.leaky.tokens.apigateway.security;

import java.util.Collections;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final String apiKey;
    private final String userId;

    public ApiKeyAuthenticationToken(String apiKey) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.userId = null;
        setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(String apiKey, String userId) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.userId = userId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}
