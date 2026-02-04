package com.leaky.tokens.apigateway.security;

import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class ApiKeyAuthenticationConverter implements ServerAuthenticationConverter {
    private final ApiKeyAuthProperties properties;

    public ApiKeyAuthenticationConverter(ApiKeyAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<org.springframework.security.core.Authentication> convert(ServerWebExchange exchange) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(properties.getHeaderName());
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(new ApiKeyAuthenticationToken(apiKey.trim()));
    }
}
