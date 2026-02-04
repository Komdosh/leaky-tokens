package com.leaky.tokens.tokenservice.tier;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TokenTierResolver {
    private final TokenTierProperties properties;

    public TokenTierResolver(TokenTierProperties properties) {
        this.properties = properties;
    }

    public TokenTierProperties.TierConfig resolveTier() {
        return resolveTier(SecurityContextHolder.getContext().getAuthentication());
    }

    public TokenTierProperties.TierConfig resolveTier(Authentication authentication) {
        Map<String, TokenTierProperties.TierConfig> levels = properties.getLevels();
        if (levels == null || levels.isEmpty()) {
            return new TokenTierProperties.TierConfig();
        }

        String defaultKey = normalize(properties.getDefaultTier());
        TokenTierProperties.TierConfig defaultTier = levels.getOrDefault(defaultKey, new TokenTierProperties.TierConfig());

        if (authentication == null) {
            return defaultTier;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return defaultTier;
        }

        Optional<TokenTierProperties.TierConfig> bestTier = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .map(this::normalizeRole)
            .map(levels::get)
            .filter(tier -> tier != null)
            .max(Comparator.comparingInt(TokenTierProperties.TierConfig::getPriority));

        return bestTier.orElse(defaultTier);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
