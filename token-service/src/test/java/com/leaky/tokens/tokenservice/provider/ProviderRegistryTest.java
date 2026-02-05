package com.leaky.tokens.tokenservice.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProviderRegistryTest {
    @Test
    void returnsBaseUrlForKnownProviderCaseInsensitive() {
        ProviderRegistry registry = new ProviderRegistry(
            "http://qwen",
            "http://gemini",
            "http://openai"
        );

        assertThat(registry.baseUrl("OPENAI")).isEqualTo("http://openai");
        assertThat(registry.baseUrl("gemini")).isEqualTo("http://gemini");
    }

    @Test
    void throwsForUnknownProvider() {
        ProviderRegistry registry = new ProviderRegistry(
            "http://qwen",
            "http://gemini",
            "http://openai"
        );

        assertThatThrownBy(() -> registry.baseUrl("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown provider");
    }
}
