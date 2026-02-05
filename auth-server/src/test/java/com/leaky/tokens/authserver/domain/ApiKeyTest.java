package com.leaky.tokens.authserver.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ApiKeyTest {
    @Test
    void onCreatePopulatesIdAndCreatedAtWhenMissing() {
        ApiKey apiKey = new ApiKey(null, UUID.randomUUID(), "hash", "cli", null, null);

        apiKey.onCreate();

        assertThat(apiKey.getId()).isNotNull();
        assertThat(apiKey.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreateKeepsExistingValues() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-02-04T10:00:00Z");
        ApiKey apiKey = new ApiKey(id, UUID.randomUUID(), "hash", "cli", createdAt, null);

        apiKey.onCreate();

        assertThat(apiKey.getId()).isEqualTo(id);
        assertThat(apiKey.getCreatedAt()).isEqualTo(createdAt);
    }
}
