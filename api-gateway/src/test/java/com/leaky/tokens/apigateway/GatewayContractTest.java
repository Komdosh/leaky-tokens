package com.leaky.tokens.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.HttpServerResponse;

@SpringBootTest(
    classes = GatewayContractTest.TestGatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false",
        "gateway.rate-limit.enabled=false",
        "gateway.api-key.enabled=true",
        "gateway.security.permit-all=true"
    }
)
class GatewayContractTest {
    private static DisposableServer mockTokenService;
    private static DisposableServer mockAnalyticsService;
    private static DisposableServer mockAuthService;
    private static final AtomicReference<Map<String, String>> lastTokenHeaders = new AtomicReference<>();
    private static final AtomicReference<Map<String, String>> lastAnalyticsHeaders = new AtomicReference<>();
    private static final AtomicReference<String> lastAuthApiKey = new AtomicReference<>();

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startMockServices() {
        mockTokenService = HttpServer.create()
            .port(0)
            .route(GatewayContractTest::tokenRoutes)
            .bindNow();
        mockAnalyticsService = HttpServer.create()
            .port(0)
            .route(GatewayContractTest::analyticsRoutes)
            .bindNow();
        mockAuthService = HttpServer.create()
            .port(0)
            .route(GatewayContractTest::authRoutes)
            .bindNow();
    }

    @AfterAll
    static void stopMockServices() {
        if (mockTokenService != null) {
            mockTokenService.disposeNow();
        }
        if (mockAnalyticsService != null) {
            mockAnalyticsService.disposeNow();
        }
        if (mockAuthService != null) {
            mockAuthService.disposeNow();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("gateway.api-key.auth-server-url", () -> "http://localhost:" + mockAuthService.port());
        registry.add("gateway.security.permit-all", () -> "true");
    }

    @Test
    void sendsApiKeyToAuthServer() {
        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        assertThat(lastAuthApiKey.get()).isEqualTo("valid-key");
    }

    @Test
    void forwardsUserHeadersToTokenServiceAndStripsApiKey() {
        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        Map<String, String> headers = lastTokenHeaders.get();
        assertThat(headers.get("X-User-Id")).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(headers.get("X-User-Roles")).isEqualTo("ADMIN,USER");
        assertThat(headers.containsKey("X-Api-Key")).isFalse();
    }

    @Test
    void forwardsUserHeadersToAnalyticsService() {
        newClient().get()
            .uri("/api/v1/analytics/usage?provider=openai&limit=5")
            .header("X-Api-Key", "valid-key")
            .exchange()
            .expectStatus().isOk();

        Map<String, String> headers = lastAnalyticsHeaders.get();
        assertThat(headers.get("X-User-Id")).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(headers.get("X-User-Roles")).isEqualTo("ADMIN,USER");
    }

    private WebTestClient newClient() {
        return WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    private static HttpServerRoutes tokenRoutes(HttpServerRoutes routes) {
        return routes.post("/api/v1/tokens/consume", (request, response) -> {
            lastTokenHeaders.set(toHeaderMap(request.requestHeaders()));
            return respondOk(response);
        });
    }

    private static HttpServerRoutes analyticsRoutes(HttpServerRoutes routes) {
        return routes.get("/api/v1/analytics/usage", (request, response) -> {
            lastAnalyticsHeaders.set(toHeaderMap(request.requestHeaders()));
            return response.status(200)
                .header("Content-Type", "application/json")
                .sendString(Mono.just("{\"provider\":\"openai\",\"count\":0,\"items\":[]}"))
                .then();
        });
    }

    private static HttpServerRoutes authRoutes(HttpServerRoutes routes) {
        return routes.get("/api/v1/auth/api-keys/validate", (request, response) -> {
            String apiKey = request.requestHeaders().get("X-Api-Key");
            lastAuthApiKey.set(apiKey);
            if (apiKey == null || apiKey.isBlank() || !apiKey.equals("valid-key")) {
                return response.status(401).send();
            }
            return response.status(200)
                .header("Content-Type", "application/json")
                .sendString(Mono.just("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"roles\":[\"ADMIN\",\"USER\"]}"))
                .then();
        });
    }

    private static Map<String, String> toHeaderMap(io.netty.handler.codec.http.HttpHeaders headers) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        headers.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        return map;
    }

    private static Mono<Void> respondOk(HttpServerResponse response) {
        return response.status(200)
            .header("Content-Type", "application/json")
            .sendString(Mono.just("{\"allowed\":true}"))
            .then();
    }

    @SpringBootApplication
    @ConfigurationPropertiesScan
    static class TestGatewayApplication {
        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                .route("token-service", route -> route
                    .path("/api/v1/tokens/**")
                    .uri("http://localhost:" + mockTokenService.port()))
                .route("analytics-service", route -> route
                    .path("/api/v1/analytics/**")
                    .uri("http://localhost:" + mockAnalyticsService.port()))
                .build();
        }

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> reactor.core.publisher.Mono.just(
                Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .build()
            );
        }
    }
}
