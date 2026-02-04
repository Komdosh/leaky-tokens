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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.HttpServerResponse;

@SpringBootTest(
    classes = GatewayRateLimitTest.TestGatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false",
        "gateway.security.permit-all=true",
        "gateway.api-key.enabled=false",
        "gateway.rate-limit.enabled=true",
        "gateway.rate-limit.key-strategy=AUTO",
        "gateway.rate-limit.capacity=5",
        "gateway.rate-limit.window-seconds=60",
        "gateway.rate-limit.routes.token-service.capacity=1",
        "gateway.rate-limit.routes.token-service.window-seconds=60",
        "gateway.rate-limit.routes.analytics-service.capacity=2",
        "gateway.rate-limit.routes.analytics-service.window-seconds=60",
        "gateway.rate-limit.whitelist-paths=/actuator/**"
    }
)
class GatewayRateLimitTest {
    private static DisposableServer mockTokenService;
    private static DisposableServer mockActuatorService;
    private static DisposableServer mockAnalyticsService;

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
                .route(GatewayRateLimitTest::tokenRoutes)
                .bindNow();
        }
        if (mockActuatorService == null) {
            mockActuatorService = HttpServer.create()
                .port(0)
                .route(GatewayRateLimitTest::actuatorRoutes)
                .bindNow();
        }
        if (mockAnalyticsService == null) {
            mockAnalyticsService = HttpServer.create()
                .port(0)
                .route(GatewayRateLimitTest::analyticsRoutes)
                .bindNow();
        }
    }

    @AfterAll
    static void stopMockService() {
        if (mockTokenService != null) {
            mockTokenService.disposeNow();
        }
        if (mockActuatorService != null) {
            mockActuatorService.disposeNow();
        }
        if (mockAnalyticsService != null) {
            mockAnalyticsService.disposeNow();
        }
    }

    @Test
    void rateLimitAppliesPerRouteOverride() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> first = webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "rate-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":1}")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        EntityExchangeResult<String> second = webTestClient.post()
            .uri("/api/v1/tokens/consume")
            .contentType(APPLICATION_JSON)
            .header("X-Api-Key", "rate-key")
            .bodyValue("{\"userId\":\"u1\",\"provider\":\"openai\",\"tokens\":1}")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(first.getStatus().value()).isEqualTo(200);
        assertThat(second.getStatus().value()).isEqualTo(429);
        assertThat(second.getResponseHeaders().getFirst("Retry-After")).isNotBlank();
    }

    @Test
    void whitelistedPathsBypassRateLimit() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        for (int i = 0; i < 5; i++) {
            EntityExchangeResult<String> result = webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

            if (i == 0) {
                assertThat(result.getResponseBody()).contains("\"status\":\"UP\"");
            }
            assertThat(result.getResponseHeaders().getFirst("X-RateLimit-Limit")).isNull();
            assertThat(result.getResponseHeaders().getFirst("Retry-After")).isNull();
        }
    }

    @Test
    void appliesDefaultLimitWhenRouteNotOverridden() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> first = webTestClient.get()
            .uri("/api/v1/analytics/usage")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        EntityExchangeResult<String> second = webTestClient.get()
            .uri("/api/v1/analytics/usage")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(first.getStatus().value()).isEqualTo(200);
        assertThat(second.getStatus().value()).isEqualTo(200);
        assertThat(second.getResponseHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("5");
    }

    @Test
    void appliesAnalyticsRouteOverride() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();

        EntityExchangeResult<String> first = webTestClient.get()
            .uri("/api/v1/analytics/usage")
            .header("X-Api-Key", "analytics-key")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        EntityExchangeResult<String> second = webTestClient.get()
            .uri("/api/v1/analytics/usage")
            .header("X-Api-Key", "analytics-key")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        EntityExchangeResult<String> third = webTestClient.get()
            .uri("/api/v1/analytics/usage")
            .header("X-Api-Key", "analytics-key")
            .exchange()
            .expectBody(String.class)
            .returnResult();

        assertThat(first.getStatus().value()).isEqualTo(200);
        assertThat(second.getStatus().value()).isEqualTo(200);
        assertThat(second.getResponseHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(third.getStatus().value()).isEqualTo(429);
        assertThat(third.getResponseHeaders().getFirst("Retry-After")).isNotBlank();
    }

    private static HttpServerRoutes tokenRoutes(HttpServerRoutes routes) {
        return routes.post("/api/v1/tokens/consume", (request, response) -> respondOk(response));
    }

    private static HttpServerRoutes actuatorRoutes(HttpServerRoutes routes) {
        return routes.get("/actuator/health", (request, response) -> response.status(200)
            .header("Content-Type", "application/json")
            .sendString(Mono.just("{\"status\":\"UP\"}"))
            .then());
    }

    private static HttpServerRoutes analyticsRoutes(HttpServerRoutes routes) {
        return routes.get("/api/v1/analytics/usage", (request, response) -> respondOk(response));
    }

    private static Mono<Void> respondOk(HttpServerResponse response) {
        return response.status(200)
            .header("Content-Type", "application/json")
            .sendString(Mono.just("{\"allowed\":true}"))
            .then();
    }

    @SpringBootApplication
    static class TestGatewayApplication {
        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                .route("token-service", route -> route
                    .path("/api/v1/tokens/**")
                    .uri("http://localhost:" + mockTokenService.port()))
                .route("actuator-service", route -> route
                    .path("/actuator/**")
                    .uri("http://localhost:" + mockActuatorService.port()))
                .route("analytics-service", route -> route
                    .path("/api/v1/analytics/**")
                    .uri("http://localhost:" + mockAnalyticsService.port()))
                .build();
        }

        @Bean
        ReactiveJwtDecoder reactiveJwtDecoder() {
            return token -> Mono.just(
                Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test")
                    .build()
            );
        }
    }
}
