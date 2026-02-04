package com.leaky.tokens.authserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.leaky.tokens.authserver.config.JwtConfig;
import com.leaky.tokens.authserver.domain.Role;
import com.leaky.tokens.authserver.domain.UserAccount;
import com.leaky.tokens.authserver.service.JwtService;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    @Test
    void issueTokenIncludesSortedRolesAndIssuer() throws Exception {
        JwtConfig config = new JwtConfig();
        JwtService service = new JwtService(config, "issuer-test", 1800);

        UserAccount user = new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hashed");
        user.addRole(new Role(UUID.randomUUID(), "USER", "User role"));
        user.addRole(new Role(UUID.randomUUID(), "ADMIN", "Admin role"));

        String token = service.issueToken(user);
        SignedJWT parsed = SignedJWT.parse(token);

        assertThat(parsed.getJWTClaimsSet().getSubject()).isEqualTo(user.getId().toString());
        assertThat(parsed.getJWTClaimsSet().getIssuer()).isEqualTo("issuer-test");
        assertThat(parsed.getJWTClaimsSet().getStringListClaim("roles")).containsExactly("ADMIN", "USER");
    }
}
