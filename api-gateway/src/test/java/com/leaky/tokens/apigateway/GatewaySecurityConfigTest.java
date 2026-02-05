package com.leaky.tokens.apigateway;

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
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.http.server.HttpServerResponse;

@SpringBootTest(
    classes = GatewaySecurityConfigTest.TestGatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false",
        "gateway.rate-limit.enabled=false",
        "gateway.api-key.enabled=false",
        "gateway.security.permit-all=false",
        "spring.main.allow-bean-definition-overriding=true"
    }
)
class GatewaySecurityConfigTest {
    private static DisposableServer mockAuthService;
    private static DisposableServer mockMiscService;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startMockServices() {
        if (mockAuthService == null) {
            mockAuthService = HttpServer.create()
                .port(0)
                .route(GatewaySecurityConfigTest::authRoutes)
                .bindNow();
        }
        if (mockMiscService == null) {
            mockMiscService = HttpServer.create()
                .port(0)
                .route(GatewaySecurityConfigTest::miscRoutes)
                .bindNow();
        }
    }

    @AfterAll
    static void stopMockServices() {
        if (mockAuthService != null) {
            mockAuthService.disposeNow();
        }
        if (mockMiscService != null) {
            mockMiscService.disposeNow();
        }
    }

    @Test
    void authEndpointsArePermittedWithoutJwt() {
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build()
            .post()
            .uri("/api/v1/auth/register")
            .bodyValue("{\"username\":\"demo\",\"email\":\"demo@example.com\",\"password\":\"password\"}")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void secureEndpointsRequireAuthentication() {
        WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build()
            .get()
            .uri("/api/v1/misc/secure")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    private static HttpServerRoutes authRoutes(HttpServerRoutes routes) {
        return routes.post("/api/v1/auth/register", (request, response) -> respondOk(response));
    }

    private static HttpServerRoutes miscRoutes(HttpServerRoutes routes) {
        return routes.get("/api/v1/misc/secure", (request, response) -> respondOk(response));
    }

    private static Mono<Void> respondOk(HttpServerResponse response) {
        return response.status(200)
            .header("Content-Type", "application/json")
            .sendString(Mono.just("{\"status\":\"ok\"}"))
            .then();
    }

    @SpringBootApplication
    @ConfigurationPropertiesScan
    static class TestGatewayApplication {
        @Bean
        RouteLocator testRoutes(RouteLocatorBuilder builder) {
            return builder.routes()
                .route("auth-server", route -> route
                    .path("/api/v1/auth/**")
                    .uri("http://localhost:" + mockAuthService.port()))
                .route("misc-service", route -> route
                    .path("/api/v1/misc/**")
                    .uri("http://localhost:" + mockMiscService.port()))
                .build();
        }
    }
}
