package com.leaky.tokens.analyticsservice.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TokenUsageByProviderKeyTest {
    @Test
    void equalsComparesProviderAndTimestamp() {
        Instant timestamp = Instant.parse("2026-02-04T16:00:00Z");
        TokenUsageByProviderKey keyA = new TokenUsageByProviderKey("openai", timestamp);
        TokenUsageByProviderKey keyB = new TokenUsageByProviderKey("openai", timestamp);
        TokenUsageByProviderKey keyC = new TokenUsageByProviderKey("gemini", timestamp);
        TokenUsageByProviderKey keyD = new TokenUsageByProviderKey("openai", Instant.parse("2026-02-04T16:01:00Z"));

        assertThat(keyA).isEqualTo(keyB);
        assertThat(keyA.hashCode()).isEqualTo(keyB.hashCode());
        assertThat(keyA).isNotEqualTo(keyC);
        assertThat(keyA).isNotEqualTo(keyD);
    }
}
