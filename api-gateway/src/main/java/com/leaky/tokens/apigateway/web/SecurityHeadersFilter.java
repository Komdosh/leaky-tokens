package com.leaky.tokens.apigateway.web;

import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpResponse response = exchange.getResponse();
        response.beforeCommit(() -> {
            response.getHeaders().set("X-Content-Type-Options", "nosniff");
            response.getHeaders().set("X-Frame-Options", "DENY");
            response.getHeaders().set("Referrer-Policy", "no-referrer");
            response.getHeaders().set("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            response.getHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            return Mono.empty();
        });
        return chain.filter(exchange);
    }
}
