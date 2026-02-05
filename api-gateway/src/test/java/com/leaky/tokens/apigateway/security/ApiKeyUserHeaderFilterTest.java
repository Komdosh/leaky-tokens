package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class ApiKeyUserHeaderFilterTest {
    @Test
    void addsUserHeadersAndStripsApiKey() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyUserHeaderFilter filter = new ApiKeyUserHeaderFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "raw-key")
                .build()
        );
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken("raw-key", "user-1", List.of("ADMIN", "USER"));
        AtomicReference<org.springframework.http.HttpHeaders> headersRef = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            headersRef.set(ex.getRequest().getHeaders());
            return ex.getResponse().setComplete();
        };

        filter.filter(exchange, chain)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))))
            .block();

        assertThat(headersRef.get().getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(headersRef.get().getFirst("X-User-Roles")).isEqualTo("ADMIN,USER");
        assertThat(headersRef.get().getFirst("X-Api-Key")).isNull();
    }

    @Test
    void usesCustomHeaderNames() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        properties.setUserHeaderName("X-Alt-User");
        properties.setRolesHeaderName("X-Alt-Roles");
        properties.setHeaderName("X-Alt-Key");
        ApiKeyUserHeaderFilter filter = new ApiKeyUserHeaderFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Alt-Key", "raw-key")
                .build()
        );
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken("raw-key", "user-2", List.of("USER"));
        AtomicReference<org.springframework.http.HttpHeaders> headersRef = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            headersRef.set(ex.getRequest().getHeaders());
            return ex.getResponse().setComplete();
        };

        filter.filter(exchange, chain)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))))
            .block();

        assertThat(headersRef.get().getFirst("X-Alt-User")).isEqualTo("user-2");
        assertThat(headersRef.get().getFirst("X-Alt-Roles")).isEqualTo("USER");
        assertThat(headersRef.get().getFirst("X-Alt-Key")).isNull();
    }

    @Test
    void skipsWhenPrincipalBlank() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyUserHeaderFilter filter = new ApiKeyUserHeaderFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "raw-key")
                .build()
        );
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken("raw-key", "  ", List.of("USER"));
        AtomicReference<org.springframework.http.HttpHeaders> headersRef = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            headersRef.set(ex.getRequest().getHeaders());
            return ex.getResponse().setComplete();
        };

        filter.filter(exchange, chain)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))))
            .block();

        assertThat(headersRef.get().getFirst("X-User-Id")).isNull();
        assertThat(headersRef.get().getFirst("X-User-Roles")).isNull();
        assertThat(headersRef.get().getFirst("X-Api-Key")).isEqualTo("raw-key");
    }

    @Test
    void removesApiKeyEvenWhenRolesEmpty() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyUserHeaderFilter filter = new ApiKeyUserHeaderFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "raw-key")
                .build()
        );
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken("raw-key", "user-3", List.of());
        AtomicReference<org.springframework.http.HttpHeaders> headersRef = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            headersRef.set(ex.getRequest().getHeaders());
            return ex.getResponse().setComplete();
        };

        filter.filter(exchange, chain)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))))
            .block();

        assertThat(headersRef.get().getFirst("X-User-Id")).isEqualTo("user-3");
        assertThat(headersRef.get().getFirst("X-User-Roles")).isNull();
        assertThat(headersRef.get().getFirst("X-Api-Key")).isNull();
    }

    @Test
    void doesNotMutateForNonApiKeyAuthentication() {
        ApiKeyAuthProperties properties = new ApiKeyAuthProperties();
        ApiKeyUserHeaderFilter filter = new ApiKeyUserHeaderFilter(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header("X-Api-Key", "raw-key")
                .build()
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken("user", "pass");
        AtomicReference<org.springframework.http.HttpHeaders> headersRef = new AtomicReference<>();
        WebFilterChain chain = ex -> {
            headersRef.set(ex.getRequest().getHeaders());
            return ex.getResponse().setComplete();
        };

        filter.filter(exchange, chain)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(authentication))))
            .block();

        assertThat(headersRef.get().getFirst("X-User-Id")).isNull();
        assertThat(headersRef.get().getFirst("X-User-Roles")).isNull();
        assertThat(headersRef.get().getFirst("X-Api-Key")).isEqualTo("raw-key");
    }
}
