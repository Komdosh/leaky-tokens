package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

class GatewayJwtDecoderConfigTest {
    @Test
    void decoderThrowsWhenNoConfigProvided() {
        GatewayJwtDecoderConfig config = new GatewayJwtDecoderConfig();
        ReactiveJwtDecoder decoder = config.reactiveJwtDecoder("", "");

        assertThatThrownBy(() -> decoder.decode("token").block())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT decoder is not configured");
    }

    @Test
    void decoderUsesJwkSetUriWhenProvided() {
        GatewayJwtDecoderConfig config = new GatewayJwtDecoderConfig();
        ReactiveJwtDecoder decoder = config.reactiveJwtDecoder("https://example.com/jwks", "");

        assertThat(decoder).isInstanceOf(NimbusReactiveJwtDecoder.class);
    }

    @Test
    void decoderUsesIssuerUriWhenProvided() {
        DisposableServer server = HttpServer.create()
            .port(0)
            .route(routes -> routes
                .get("/.well-known/openid-configuration", (request, response) -> {
                    String issuer = "http://localhost:" + request.hostPort();
                    String body = "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"" + issuer + "/oauth2/jwks\"}";
                    return response.status(200)
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just(body))
                        .then();
                })
                .get("/oauth2/jwks", (_, response) ->
                    response.status(200)
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just("{\"keys\":[]}"))
                        .then()
                ))
            .bindNow();

        try {
            String issuer = "http://localhost:" + server.port();
            GatewayJwtDecoderConfig config = new GatewayJwtDecoderConfig();
            ReactiveJwtDecoder decoder = config.reactiveJwtDecoder("", issuer);

            assertThat(decoder).isNotNull();
        } finally {
            server.disposeNow();
        }
    }
}
