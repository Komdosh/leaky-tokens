package com.leaky.tokens.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.DisposableServer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest(
    classes = GatewayConsumeRouteTest.TestGatewayApplication.class,
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
class GatewayConsumeRouteTest {
    private static DisposableServer mockTokenService;
    private static DisposableServer mockAnalyticsService;
    private static DisposableServer mockAuthService;
    private static final java.util.concurrent.atomic.AtomicInteger authCallCount =
        new java.util.concurrent.atomic.AtomicInteger(0);

    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    static {
        startMockService();
    }

    @BeforeAll
    static void startMockService() {
        if (mockTokenService == null) {
            mockTokenService = HttpServer.create()
                .port(0)
                .route(GatewayConsumeRouteTest::routes)
                .bindNow();
        }
        if (mockAnalyticsService == null) {
            mockAnalyticsService = HttpServer.create()
                .port(0)
                .route(GatewayConsumeRouteTest::analyticsRoutes)
                .bindNow();
        }
        if (mockAuthService == null) {
            mockAuthService = HttpServer.create()
                .port(0)
                .route(GatewayConsumeRouteTest::authRoutes)
                .bindNow();
        }
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("gateway.api-key.auth-server-url", () -> "http://localhost:" + mockAuthService.port());
        registry.add("gateway.security.permit-all", () -> "true");
    }

    @AfterAll
    static void stopMockService() {
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

    @Test
    void forwardsConsumeToTokenService() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> result = webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(result.getStatus().value())
            .withFailMessage("Expected 200 but got %s. Body: %s", result.getStatus(), result.getResponseBody())
            .isEqualTo(200);
        assertThat(result.getResponseHeaders().getContentType()).isEqualTo(APPLICATION_JSON);
        assertThat(result.getResponseHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(result.getResponseHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(result.getResponseBody()).contains("\"allowed\":true");
        assertThat(result.getResponseBody()).contains("\"userId\":\"00000000-0000-0000-0000-000000000001\"");
        assertThat(authCallCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rejectsInvalidApiKey() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> result = webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "invalid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(result.getStatus().value()).isEqualTo(401);
        assertThat(authCallCount.get()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void reusesCachedApiKeyValidation() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        authCallCount.set(0);

        webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .header("X-Drop-Auth", "true")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        assertThat(authCallCount.get()).isEqualTo(1);
    }

    @Test
    void returnsFallbackWhenCircuitBreakerTrips() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> result = webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .header("X-Trigger-Fail", "true")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(result.getStatus().value())
            .withFailMessage("Expected 503 but got %s. Body: %s", result.getStatus(), result.getResponseBody())
            .isEqualTo(503);
        assertThat(result.getResponseBody()).contains("\"service\":\"token-service\"");
        assertThat(result.getResponseBody()).contains("\"message\":\"Circuit breaker open\"");
    }

    @Test
    void returnsFallbackWhenAnalyticsCircuitBreakerTrips() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> result = webTestClient.get()
            .uri("/api/v1/analytics/usage?provider=openai&limit=5")
            .header("X-Api-Key", "valid-key")
            .header("X-Trigger-Fail-Analytics", "true")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(result.getStatus().value())
            .withFailMessage("Expected 503 but got %s. Body: %s", result.getStatus(), result.getResponseBody())
            .isEqualTo(503);
        assertThat(result.getResponseBody()).contains("\"service\":\"analytics-service\"");
    }

    private static HttpServerRoutes routes(HttpServerRoutes routes) {
        return routes.post("/api/v1/tokens/consume", (request, response) -> {
            String failHeader = request.requestHeaders().get("X-Trigger-Fail");
            if ("true".equalsIgnoreCase(failHeader)) {
                return response.status(500).sendString(Mono.just("boom")).then();
            }
            String userId = request.requestHeaders().get("X-User-Id");
            return respondOk(response, userId);
        });
    }

    private static HttpServerRoutes analyticsRoutes(HttpServerRoutes routes) {
        return routes.get("/api/v1/analytics/usage", (request, response) -> {
            String failHeader = request.requestHeaders().get("X-Trigger-Fail-Analytics");
            if ("true".equalsIgnoreCase(failHeader)) {
                return response.status(500).sendString(Mono.just("boom")).then();
            }
            return response.status(200)
                .header("Content-Type", "application/json")
                .sendString(Mono.just("{\"provider\":\"openai\",\"count\":0,\"items\":[]}"))
                .then();
        });
    }

    private static HttpServerRoutes authRoutes(HttpServerRoutes routes) {
        return routes.get("/api/v1/auth/api-keys/validate", (request, response) -> {
            String apiKey = request.requestHeaders().get("X-Api-Key");
            String dropHeader = request.requestHeaders().get("X-Drop-Auth");
            authCallCount.incrementAndGet();
            if ("true".equalsIgnoreCase(dropHeader)) {
                return response.status(503).send();
            }
            if (apiKey == null || apiKey.isBlank() || !apiKey.equals("valid-key")) {
                return response.status(401).send();
            }
            return response.status(200)
                .header("Content-Type", "application/json")
                .sendString(Mono.just("{\"userId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .then();
        });
    }

    private static Mono<Void> respondOk(HttpServerResponse response, String userId) {
        String userField = userId == null ? "" : ",\"userId\":\"" + userId + "\"";
        return response.status(200)
            .header("Content-Type", "application/json")
            .sendString(Mono.just("{\"allowed\":true,\"capacity\":1000,\"used\":100,\"remaining\":900,\"waitSeconds\":0" + userField + "}"))
            .then();
    }

    @SpringBootApplication
    static class TestGatewayApplication {
        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                .route("token-service", route -> route
                    .path("/api/v1/tokens/**")
                    .filters(filters -> filters
                        .circuitBreaker(config -> config
                            .setName("tokenServiceCB")
                            .setFallbackUri("forward:/fallback/token-service")
                            .addStatusCode("500")))
                    .uri("http://localhost:" + mockTokenService.port()))
                .route("analytics-service", route -> route
                    .path("/api/v1/analytics/**")
                    .filters(filters -> filters
                        .circuitBreaker(config -> config
                            .setName("analyticsServiceCB")
                            .setFallbackUri("forward:/fallback/analytics-service")
                            .addStatusCode("500")))
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
