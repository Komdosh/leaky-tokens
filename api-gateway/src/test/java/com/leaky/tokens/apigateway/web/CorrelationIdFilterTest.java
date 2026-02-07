package com.leaky.tokens.apigateway.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    @Test
    void usesIncomingHeader() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
            .header(CorrelationIdFilter.HEADER_NAME, "corr-gw")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> mdcValue = new AtomicReference<>();
        AtomicReference<String> requestHeader = new AtomicReference<>();

        WebFilterChain chain = webExchange -> {
            requestHeader.set(webExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME));
            mdcValue.set(MDC.get("correlationId"));
            return webExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertThat(mdcValue.get()).isEqualTo("corr-gw");
        assertThat(requestHeader.get()).isEqualTo("corr-gw");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME)).isEqualTo("corr-gw");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void generatesHeaderWhenMissing() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<String> mdcValue = new AtomicReference<>();
        AtomicReference<String> requestHeader = new AtomicReference<>();

        WebFilterChain chain = webExchange -> {
            requestHeader.set(webExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME));
            mdcValue.set(MDC.get("correlationId"));
            return webExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        String header = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
        assertThat(requestHeader.get()).isEqualTo(header);
        assertThat(mdcValue.get()).isEqualTo(header);
        assertThat(MDC.get("correlationId")).isNull();
    }
}
