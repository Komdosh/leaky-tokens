package com.leaky.tokens.tokenservice.tier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class TokenTierResolverTest {
    @Test
    void resolvesHighestPriorityTierFromAuthorities() {
        TokenTierProperties properties = new TokenTierProperties();
        properties.setDefaultTier("USER");
        TokenTierProperties.TierConfig user = new TokenTierProperties.TierConfig();
        user.setPriority(10);
        TokenTierProperties.TierConfig admin = new TokenTierProperties.TierConfig();
        admin.setPriority(100);
        properties.getLevels().put("USER", user);
        properties.getLevels().put("ADMIN", admin);

        TokenTierResolver resolver = new TokenTierResolver(properties);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "user",
            "pass",
            List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        TokenTierProperties.TierConfig resolved = resolver.resolveTier(auth);

        assertThat(resolved).isSameAs(admin);
    }

    @Test
    void fallsBackToDefaultTierWhenNoAuthorities() {
        TokenTierProperties properties = new TokenTierProperties();
        properties.setDefaultTier("USER");
        TokenTierProperties.TierConfig user = new TokenTierProperties.TierConfig();
        user.setPriority(10);
        properties.getLevels().put("USER", user);

        TokenTierResolver resolver = new TokenTierResolver(properties);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            "user",
            "pass",
            List.of()
        );

        TokenTierProperties.TierConfig resolved = resolver.resolveTier(auth);

        assertThat(resolved).isSameAs(user);
    }

    @Test
    void returnsEmptyTierWhenNoLevelsConfigured() {
        TokenTierProperties properties = new TokenTierProperties();
        properties.getLevels().clear();
        TokenTierResolver resolver = new TokenTierResolver(properties);

        TokenTierProperties.TierConfig resolved = resolver.resolveTier(null);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getPriority()).isEqualTo(0);
    }
}
