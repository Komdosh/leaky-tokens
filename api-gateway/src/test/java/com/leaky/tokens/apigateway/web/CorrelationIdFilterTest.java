package com.leaky.tokens.apigateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {
    @Test
    void echoesProvidedCorrelationId() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(CorrelationIdFilter.HEADER_NAME, "trace-123")
                .build()
        );
        WebFilterChain chain = ex -> ex.getResponse().setComplete();

        filter.filter(exchange, chain).block();
        exchange.getResponse().setComplete().block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME)).isEqualTo("trace-123");
    }

    @Test
    void generatesCorrelationIdWhenMissing() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());
        WebFilterChain chain = ex -> ex.getResponse().setComplete();

        filter.filter(exchange, chain).block();
        exchange.getResponse().setComplete().block();

        String header = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
    }
}
