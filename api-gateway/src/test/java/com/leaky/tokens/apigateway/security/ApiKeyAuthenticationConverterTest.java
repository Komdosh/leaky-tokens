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

    @Test
    void respectsCustomHeaderName() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setHeaderName("X-Alt-Key");
        ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Alt-Key", "alt-key")
                .build()
        );

        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) converter.convert(exchange).block();

        assertThat(token).isNotNull();
        assertThat(token.getCredentials()).isEqualTo("alt-key");
    }

    @Test
    void ignoresDefaultHeaderWhenCustomHeaderConfigured() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setHeaderName("X-Alt-Key");
        ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "raw-key")
                .build()
        );

        assertThat(converter.convert(exchange).blockOptional()).isEmpty();
    }

    @Test
    void usesFirstHeaderValueWhenMultipleProvided() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyAuthenticationConverter converter = new ApiKeyAuthenticationConverter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "  first-key  ", "second-key")
                .build()
        );

        ApiKeyAuthenticationToken token = (ApiKeyAuthenticationToken) converter.convert(exchange).block();

        assertThat(token).isNotNull();
        assertThat(token.getCredentials()).isEqualTo("first-key");
    }
}
