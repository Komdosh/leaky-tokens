package com.leaky.tokens.apigateway.security;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    private final String apiKey;
    private final String userId;
    private final List<String> roles;

    public ApiKeyAuthenticationToken(String apiKey) {
        super(Collections.emptyList());
        this.apiKey = apiKey;
        this.userId = null;
        this.roles = List.of();
        setAuthenticated(false);
    }

    public ApiKeyAuthenticationToken(String apiKey, String userId, List<String> roles) {
        super(authoritiesFromRoles(roles));
        this.apiKey = apiKey;
        this.userId = userId;
        this.roles = roles == null ? List.of() : List.copyOf(roles);
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

    public List<String> getRoles() {
        return roles;
    }

    private static List<GrantedAuthority> authoritiesFromRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
            .filter(role -> role != null && !role.isBlank())
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
}
