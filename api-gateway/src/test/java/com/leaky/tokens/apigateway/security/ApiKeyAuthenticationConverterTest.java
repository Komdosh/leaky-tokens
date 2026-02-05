package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class ApiKeyAuthenticationConverterTest {
    @Test
    void returnsEmptyWhenHeaderMissing() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        assertThat(converter.convert(exchange).blockOptional()).isEmpty();
    }

    @Test
    void returnsEmptyWhenHeaderBlank() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "   ")
                .build()
        );

        assertThat(converter.convert(exchange).blockOptional()).isEmpty();
    }

    @Test
    void trimsHeaderValue() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "  key-123  ")
                .build()
        );

        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) converter.convert(exchange).block();

        assertThat(token).isNotNull();
        assertThat(token.getCredentials()).isEqualTo("key-123");
    }
}
