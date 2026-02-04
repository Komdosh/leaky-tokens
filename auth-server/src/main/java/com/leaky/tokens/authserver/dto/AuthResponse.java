package com.leaky.tokens.authserver.dto;

import java.util.List;
import java.util.UUID;

public class AuthResponse {
    private final UUID userId;
    private final String username;
    private final String token;
    private final List<String> roles;

    public AuthResponse(UUID userId, String username, String token, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.token = token;
        this.roles = roles;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public List<String> getRoles() {
        return roles;
    }
}
