package com.leaky.tokens.apigateway.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ApiKeyUserHeaderFilter implements WebFilter {
    private final ApiKeyAuthProperties properties;

    public ApiKeyUserHeaderFilter(ApiKeyAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(context -> context.getAuthentication())
            .flatMap(authentication -> chain.filter(maybeAddUserHeader(exchange, authentication)))
            .switchIfEmpty(chain.filter(exchange));
    }

    private ServerWebExchange maybeAddUserHeader(ServerWebExchange exchange, Authentication authentication) {
        if (!(authentication instanceof ApiKeyAuthenticationToken token)) {
            return exchange;
        }
        Object principal = token.getPrincipal();
        if (principal == null) {
            return exchange;
        }
        String userId = String.valueOf(principal);
        if (userId.isBlank()) {
            return exchange;
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(properties.getUserHeaderName(), userId)
            .build();
        return exchange.mutate().request(mutated).build();
    }
}
