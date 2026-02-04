package com.leaky.tokens.authserver.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.leaky.tokens.authserver.config.JwtConfig;
import com.leaky.tokens.authserver.domain.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final String issuer;
    private final Duration expiry;
    private final JwtConfig jwtConfig;

    public JwtService(
        JwtConfig jwtConfig,
        @Value("${auth.jwt.issuer:leaky-tokens-auth}") String issuer,
        @Value("${auth.jwt.expiry-seconds:3600}") long expirySeconds
    ) {
        this.jwtConfig = jwtConfig;
        this.issuer = issuer;
        this.expiry = Duration.ofSeconds(expirySeconds);
    }

    public String issueToken(UserAccount user) {
        Instant expiresAt = Instant.now().plus(expiry);
        List<String> roles = user.getRoles().stream()
            .map(role -> role.getName())
            .sorted()
            .toList();
        return jwtConfig.signToken(user.getId().toString(), issuer, expiresAt, roles);
    }

    public String getIssuer() {
        return issuer;
    }

    public Duration getExpiry() {
        return expiry;
    }
}
