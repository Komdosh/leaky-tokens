package com.leaky.tokens.authserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.leaky.tokens.authserver.config.JwtConfig;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

class JwtConfigTest {
    @Test
    void signTokenIncludesClaimsAndRoles() throws Exception {
        JwtConfig config = new JwtConfig();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        String token = config.signToken("user-123", "issuer-test", expiresAt, List.of("ADMIN", "USER"));
        SignedJWT parsed = SignedJWT.parse(token);

        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo("user-123");
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("issuer-test");
        assertThat(parsed.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("ADMIN", "USER");

        Instant exp = parsed.getJWTClaimsSet().getExpirationTime().toInstant();
        assertThat(Duration.between(expiresAt, exp).abs().toSeconds()).isLessThan(2);
    }

    @Test
    void signTokenDefaultsToEmptyRoles() throws Exception {
        JwtConfig config = new JwtConfig();
        Instant expiresAt = Instant.now().plusSeconds(300);

        String token = config.signToken("user-456", "issuer-test", expiresAt, null);
        SignedJWT parsed = SignedJWT.parse(token);

        assertThat(parsed.getJWTClaimsSet().getStringListClaim("roles")).isEmpty();
    }
}
