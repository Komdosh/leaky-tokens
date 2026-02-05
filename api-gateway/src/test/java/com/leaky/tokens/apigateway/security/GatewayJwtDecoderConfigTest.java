package com.leaky.tokens.apigateway.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

class GatewayJwtDecoderConfigTest {
    @Test
    void decoderThrowsWhenNoConfigProvided() {
        GatewayJwtDecoderConfig config = new GatewayJwtDecoderConfig();
        ReactiveJwtDecoder decoder = config.reactiveJwtDecoder("", "");

        assertThatThrownBy(() -> decoder.decode("token").block())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT decoder is not configured");
    }
}
