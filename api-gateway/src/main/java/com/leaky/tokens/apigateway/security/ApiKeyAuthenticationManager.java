package com.leaky.tokens.apigateway.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class ApiKeyAuthenticationManager implements ReactiveAuthenticationManager {
    private final WebClient webClient;
    private final ApiKeyAuthProperties properties;
    private final ApiKeyValidationCache cache;
    private final GatewayMetrics metrics;

    public ApiKeyAuthenticationManager(WebClient.Builder builder,
                                       ApiKeyAuthProperties properties,
                                       ApiKeyValidationCache cache,
                                       GatewayMetrics metrics) {
        this.webClient = builder.baseUrl(properties.getAuthServerUrl()).build();
        this.properties = properties;
        this.cache = cache;
        this.metrics = metrics;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof ApiKeyAuthenticationToken token)) {
            return Mono.empty();
        }
        String apiKey = String.valueOf(token.getCredentials());
        Optional<ApiKeyValidationCache.CacheEntry> cached =
            cache.get(apiKey, properties.getCacheTtlSeconds());
        if (cached.isPresent()) {
            metrics.apiKeyCache("hit");
            metrics.apiKeyValidation("success");
            return Mono.just(new ApiKeyAuthenticationToken(apiKey, cached.get().getUserId(), cached.get().getRoles()));
        }
        metrics.apiKeyCache("miss");
        return webClient.get()
            .uri("/api/v1/auth/api-keys/validate")
            .header(properties.getHeaderName(), apiKey)
            .retrieve()
            .onStatus(status -> status.isError(), response -> {
                metrics.apiKeyValidation("failure");
                return Mono.error(new BadCredentialsException("invalid api key"));
            })
            .bodyToMono(Map.class)
            .map(body -> {
                String userId = String.valueOf(body.get("userId"));
                Instant expiresAt = null;
                Object expires = body.get("expiresAt");
                if (expires instanceof String expiresText && !expiresText.isBlank()) {
                    try {
                        expiresAt = Instant.parse(expiresText);
                    } catch (Exception ignored) {
                    }
                }
                List<String> roles = extractRoles(body.get("roles"));
                cache.put(apiKey, new ApiKeyValidationCache.CacheEntry(userId, expiresAt, roles, Instant.now()),
                    properties.getCacheMaxSize());
                metrics.apiKeyValidation("success");
                return new ApiKeyAuthenticationToken(apiKey, userId, roles);
            });
    }

    private List<String> extractRoles(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .map(String::valueOf)
                .filter(role -> !role.isBlank())
                .toList();
        }
        return List.of();
    }
}
