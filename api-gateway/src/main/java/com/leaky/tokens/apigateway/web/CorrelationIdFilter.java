package com.leaky.tokens.apigateway.web;

import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements WebFilter {
    public static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    public @NonNull Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        MDC.put("correlationId", finalCorrelationId);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(HEADER_NAME, finalCorrelationId)
            .build();

        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HEADER_NAME, finalCorrelationId);
            return Mono.empty();
        });
        return chain.filter(exchange.mutate().request(mutated).build())
            .doFinally(_ -> MDC.remove("correlationId"));
    }
}
