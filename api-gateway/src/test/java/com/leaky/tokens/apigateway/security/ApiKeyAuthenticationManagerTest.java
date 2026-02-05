package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

class ApiKeyAuthenticationManagerTest {
    @Test
    void returnsCachedEntryWithoutCallingAuthServer() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        cache.put("cached-key", new ApiKeyValidationCache.CacheEntry(
            "user-1",
            Instant.now().plusSeconds(60),
            List.of("USER"),
            Instant.now()
        ), 100);

        WebClient.Builder builder = WebClient.builder().exchangeFunction(_ -> Mono.error(new IllegalStateException("should not call")));
        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("cached-key");
        ApiKeyAuthenticationToken auth = (ApiKeyAuthenticationToken) manager.authenticate(token).block();

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("user-1");
        assertThat(auth.getRoles()).containsExactly("USER");
    }

    @Test
    void failsWhenAuthServerRejectsKey() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(rejectingExchange());

        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("bad-key");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> manager.authenticate(token).block())
            .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void cachesAuthServerResponse() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        properties.setCacheMaxSize(10);
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(successExchange());

        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("fresh-key");
        ApiKeyAuthenticationToken auth = (ApiKeyAuthenticationToken) manager.authenticate(token).block();

        assertThat(auth.getPrincipal()).isEqualTo("user-123");
        assertThat(cache.get("fresh-key", 60)).isPresent();
    }

    @Test
    void handlesNonListRolesAsEmpty() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(nonListRolesExchange());

        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("role-key");
        ApiKeyAuthenticationToken auth = (ApiKeyAuthenticationToken) manager.authenticate(token).block();

        assertThat(auth.getRoles()).isEmpty();
    }

    @Test
    void ignoresNonApiKeyAuthentication() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(successExchange());

        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        var result = manager.authenticate(new UsernamePasswordAuthenticationToken("user", "pass")).blockOptional();

        assertThat(result).isEmpty();
    }

    @Test
    void ignoresInvalidExpiresAt() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        properties.setCacheMaxSize(10);
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(invalidExpiresExchange());

        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("bad-exp-key");
        ApiKeyAuthenticationToken auth = (ApiKeyAuthenticationToken) manager.authenticate(token).block();

        assertThat(auth).isNotNull();
        assertThat(cache.get("bad-exp-key", 60)).isPresent();
        assertThat(cache.get("bad-exp-key", 60).get().getExpiresAt()).isNull();
    }

    @Test
    void filtersBlankRolesFromResponse() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setAuthServerUrl("http://localhost");
        ApiKeyValidationCache cache = new ApiKeyValidationCache();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(blankRolesExchange());

        ApiKeyAuthenticationManager manager = new ApiKeyAuthenticationManager(
            builder,
            properties,
            cache,
            new GatewayMetrics(new SimpleMeterRegistry())
        );

        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken("role-key");
        ApiKeyAuthenticationToken auth = (ApiKeyAuthenticationToken) manager.authenticate(token).block();

        assertThat(auth.getRoles()).containsExactly("ADMIN");
    }

    private ExchangeFunction rejectingExchange() {
        return _ -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build());
    }

    private ExchangeFunction successExchange() {
        return _ -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"userId\":\"user-123\",\"roles\":[\"ADMIN\"],\"expiresAt\":\"2026-12-31T00:00:00Z\"}")
                .build()
        );
    }

    private ExchangeFunction nonListRolesExchange() {
        return _ -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"userId\":\"user-123\",\"roles\":\"ADMIN\"}")
                .build()
        );
    }

    private ExchangeFunction invalidExpiresExchange() {
        return _ -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"userId\":\"user-123\",\"roles\":[\"USER\"],\"expiresAt\":\"not-a-date\"}")
                .build()
        );
    }

    private ExchangeFunction blankRolesExchange() {
        return _ -> Mono.just(
            ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("{\"userId\":\"user-123\",\"roles\":[\"ADMIN\",\" \"]}")
                .build()
        );
    }
}
