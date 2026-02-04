package com.leaky.tokens.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayJwtDecoderConfig {
    @Bean
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    public ReactiveJwtDecoder reactiveJwtDecoder(
        @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
        @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri
    ) {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        if (issuerUri != null && !issuerUri.isBlank()) {
            return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
        }
        return token -> Mono.error(new IllegalStateException("JWT decoder is not configured"));
    }
}
