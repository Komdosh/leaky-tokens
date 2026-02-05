package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ApiKeyAuthenticationTokenTest {
    @Test
    void unauthenticatedTokenHasNoPrincipalOrRoles() {
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("raw-key");

        assertThat(token.isAuthenticated()).isFalse();
        assertThat(token.getPrincipal()).isNull();
        assertThat(token.getRoles()).isEmpty();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void rolesAreNormalizedAndFiltered() {
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(
            "raw-key",
            "user-1",
            List.of("ADMIN", "ROLE_USER", " ")
        );

        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getRoles()).containsExactly("ADMIN", "ROLE_USER", " ");
        assertThat(token.getAuthorities()).containsExactly(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_USER")
        );
    }
}
