package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApiKeyValidationCacheTest {
    @Test
    void expiresEntriesByTtl() {
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        ApiKeyValidationCache.CacheEntry entry = new ApiKeyValidationCache.CacheEntry(
            "user-1",
            null,
            List.of("USER"),
            Instant.now().minusSeconds(5)
        );

        cache.put("key-1", entry, 10);

        assertThat(cache.get("key-1", 1)).isEmpty();
        assertThat(cache.get("key-1", 0)).isEmpty();
    }

    @Test
    void expiresWhenKeyExpires() {
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        ApiKeyValidationCache.CacheEntry entry = new ApiKeyValidationCache.CacheEntry(
            "user-1",
            Instant.now().minusSeconds(1),
            List.of("USER"),
            Instant.now()
        );

        cache.put("key-2", entry, 10);

        assertThat(cache.get("key-2", 120)).isEmpty();
    }

    @Test
    void clearsCacheWhenMaxSizeReached() {
        ApiKeyValidationCache cache = new ApiKeyValidationCache();

        cache.put("key-1", new ApiKeyValidationCache.CacheEntry("u1", null, List.of(), Instant.now()), 1);
        cache.put("key-2", new ApiKeyValidationCache.CacheEntry("u2", null, List.of(), Instant.now()), 1);

        assertThat(cache.get("key-1", 120)).isEmpty();
        assertThat(cache.get("key-2", 120)).isPresent();
    }
}
