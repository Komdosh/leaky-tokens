package com.leaky.tokens.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
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
        "gateway.security.permit-all=true",
        "gateway.api-key.cache-ttl-seconds=1"
    }
)
class GatewayConsumeRouteTest {
    private static DisposableServer mockTokenService;
    private static DisposableServer mockAnalyticsService;
    private static DisposableServer mockAuthService;
    private static final java.util.concurrent.atomic.AtomicInteger authCallCount =
        new java.util.concurrent.atomic.AtomicInteger(0);
    private static final java.util.concurrent.atomic.AtomicBoolean invalidateNextAuth =
        new java.util.concurrent.atomic.AtomicBoolean(false);

    private WebTestClient webTestClient;

    @Autowired
    private com.leaky.tokens.apigateway.security.ApiKeyAuthProperties apiKeyAuthProperties;

    @Autowired
    private com.leaky.tokens.apigateway.security.ApiKeyValidationCache apiKeyValidationCache;

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
        registry.add("gateway.api-key.cache-ttl-seconds", () -> "1");
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

    @BeforeEach
    void resetCache() {
        apiKeyAuthProperties.setCacheTtlSeconds(1);
        try {
            java.lang.reflect.Field cacheField =
                com.leaky.tokens.apigateway.security.ApiKeyValidationCache.class.getDeclaredField("cache");
            cacheField.setAccessible(true);
            Object cache = cacheField.get(apiKeyValidationCache);
            if (cache instanceof java.util.Map<?, ?> map) {
                map.clear();
            }
        } catch (Exception ignored) {
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
        authCallCount.set(0);

        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        newClient().post()
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
    void refreshesCacheAfterTtl() throws Exception {
        authCallCount.set(0);

        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        Thread.sleep(1500);

        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        assertThat(authCallCount.get()).isEqualTo(2);
    }

    @Test
    void rejectsApiKeyAfterCacheExpires() throws Exception {
        authCallCount.set(0);

        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isOk();

        invalidateNextAuth.set(true);
        Thread.sleep(1500);

        newClient().post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "valid-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":100}")
            .exchange()
            .expectStatus().isUnauthorized();

        assertThat(authCallCount.get()).isEqualTo(2);
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

    @Test
    void propagatesUserHeadersAndStripsApiKey() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> result = webTestClient.get()
            .uri("/api/v1/tokens/headers")
            .header("X-Api-Key", "valid-key")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(result.getStatus().value())
            .withFailMessage("Expected 200 but got %s. Body: %s", result.getStatus(), result.getResponseBody())
            .isEqualTo(200);
        assertThat(result.getResponseBody()).contains("\"userId\":\"00000000-0000-0000-0000-000000000001\"");
        assertThat(result.getResponseBody()).contains("\"roles\":\"ADMIN,USER\"");
        assertThat(result.getResponseBody()).contains("\"apiKeyPresent\":false");
    }

    private static HttpServerRoutes routes(HttpServerRoutes routes) {
        return routes.post("/api/v1/tokens/consume", (request, response) -> {
            String failHeader = request.requestHeaders().get("X-Trigger-Fail");
            if ("true".equalsIgnoreCase(failHeader)) {
                return response.status(500).sendString(Mono.just("boom")).then();
            }
            String userId = request.requestHeaders().get("X-User-Id");
            return respondOk(response, userId);
        }).get("/api/v1/tokens/headers", (request, response) -> {
            String userId = request.requestHeaders().get("X-User-Id");
            String roles = request.requestHeaders().get("X-User-Roles");
            boolean apiKeyPresent = request.requestHeaders().contains("X-Api-Key");
            String payload = "{\"userId\":\"" + userId + "\",\"roles\":\"" + roles + "\",\"apiKeyPresent\":" + apiKeyPresent + "}";
            return response.status(200)
                .header("Content-Type", "application/json")
                .sendString(Mono.just(payload))
                .then();
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
            if (invalidateNextAuth.getAndSet(false)) {
                return response.status(401).send();
            }
            if (apiKey == null || apiKey.isBlank() || !apiKey.equals("valid-key")) {
                return response.status(401).send();
            }
            return response.status(200)
                .header("Content-Type", "application/json")
                .sendString(Mono.just("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"roles\":[\"ADMIN\",\"USER\"]}"))
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

    private WebTestClient newClient() {
        return WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @SpringBootApplication
    @ConfigurationPropertiesScan
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
