package com.leaky.tokens.apigateway.security;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ApiKeyValidationCache {
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Optional<CacheEntry> get(String apiKey, long ttlSeconds) {
        CacheEntry entry = cache.get(apiKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(ttlSeconds)) {
            cache.remove(apiKey);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    public void put(String apiKey, CacheEntry entry, long maxSize) {
        if (maxSize <= 0) {
            return;
        }
        if (cache.size() >= maxSize) {
            cache.clear();
        }
        cache.put(apiKey, entry);
    }

    public static final class CacheEntry {
        private final String userId;
        private final Instant expiresAt;
        private final java.util.List<String> roles;
        private final Instant cachedAt;

        public CacheEntry(String userId, Instant expiresAt, java.util.List<String> roles, Instant cachedAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.roles = roles == null ? java.util.List.of() : java.util.List.copyOf(roles);
            this.cachedAt = cachedAt;
        }

        public String getUserId() {
            return userId;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public java.util.List<String> getRoles() {
            return roles;
        }

        public Instant getCachedAt() {
            return cachedAt;
        }

        private boolean isExpired(long ttlSeconds) {
            Instant now = Instant.now();
            if (expiresAt != null && expiresAt.isBefore(now)) {
                return true;
            }
            if (ttlSeconds <= 0) {
                return false;
            }
            return cachedAt.plusSeconds(ttlSeconds).isBefore(now);
        }
    }
}
