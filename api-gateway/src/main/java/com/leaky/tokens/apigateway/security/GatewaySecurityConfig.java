package com.leaky.tokens.apigateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.leaky.tokens.apigateway.metrics.GatewayMetrics;
import com.leaky.tokens.apigateway.flags.GatewayFeatureFlags;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                           ApiKeyAuthProperties apiKeyProperties,
                                                           GatewaySecurityProperties securityProperties,
                                                           ApiKeyValidationCache cache,
                                                           GatewayMetrics metrics,
                                                           GatewayFeatureFlags featureFlags,
                                                           WebClient.Builder webClientBuilder) {
        if (apiKeyProperties.isEnabled() && featureFlags.isApiKeyValidation()) {
            AuthenticationWebFilter apiKeyFilter = new AuthenticationWebFilter(
                new ApiKeyAuthenticationManager(webClientBuilder, apiKeyProperties, cache, metrics)
            );
            apiKeyFilter.setServerAuthenticationConverter(new ApiKeyAuthenticationConverter(apiKeyProperties));
            apiKeyFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());
            apiKeyFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.anyExchange());
            apiKeyFilter.setAuthenticationFailureHandler(unauthorizedHandler());
            http.addFilterBefore(apiKeyFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        }

        ServerHttpSecurity configured = http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (securityProperties.isPermitAll()) {
            configured.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        } else {
            configured.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .pathMatchers(HttpMethod.GET, "/actuator/**").permitAll()
                .anyExchange().authenticated()
            );
        }

        return configured
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .build();
    }

    private ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    private ServerAuthenticationFailureHandler unauthorizedHandler() {
        return (exchange, ex) -> {
            ServerWebExchange webExchange = exchange.getExchange();
            webExchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return webExchange.getResponse().setComplete();
        };
    }
}
