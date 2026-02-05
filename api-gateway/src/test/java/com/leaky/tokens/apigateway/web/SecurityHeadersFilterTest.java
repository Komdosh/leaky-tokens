package com.leaky.tokens.apigateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class SecurityHeadersFilterTest {
    @Test
    void addsSecurityHeaders() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        WebFilterChain chain = ex -> ex.getResponse().setComplete();

        filter.filter(exchange, chain).block();
        exchange.getResponse().setComplete().block();

        var headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(headers.getFirst("Permissions-Policy")).isEqualTo("geolocation=(), microphone=(), camera=()");
        assertThat(headers.getFirst("Strict-Transport-Security")).contains("max-age=31536000");
    }
}
