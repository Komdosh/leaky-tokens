package com.leaky.tokens.apigateway.security;

import com.leaky.tokens.apigateway.flags.GatewayFeatureFlags;
import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    GatewaySecurityConfig.class,
    GatewaySecurityConfigPermitAllTest.TestConfig.class
})
class GatewaySecurityConfigPermitAllTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
            .apply(springSecurity())
            .configureClient()
            .build();
    }

    @Test
    void permits_all_requests_when_flag_enabled() {
        webTestClient.get().uri("/api/v1/secure")
            .exchange()
            .expectStatus().isOk();
    }

    @Configuration
    @EnableWebFlux
    static class TestConfig {
        @Bean
        ApiKeyAuthProperties apiKeyAuthProperties() {
            ApiKeyAuthProperties props = new ApiKeyAuthProperties();
            props.setEnabled(false);
            return props;
        }

        @Bean
        GatewaySecurityProperties gatewaySecurityProperties() {
            GatewaySecurityProperties props = new GatewaySecurityProperties();
            props.setPermitAll(true);
            return props;
        }

        @Bean
        ApiKeyValidationCache apiKeyValidationCache() {
            return new ApiKeyValidationCache();
        }

        @Bean
        GatewayFeatureFlags gatewayFeatureFlags() {
            GatewayFeatureFlags flags = new GatewayFeatureFlags();
            flags.setApiKeyValidation(false);
            flags.setRateLimiting(false);
            return flags;
        }

        @Bean
        GatewayMetrics gatewayMetrics() {
            return new GatewayMetrics(new SimpleMeterRegistry());
        }

        @Bean
        WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "user")
                .claim("roles", java.util.List.of("USER"))
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300))
                .build());
        }

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/v1/secure")
        String secure() {
            return "ok";
        }
    }

}
