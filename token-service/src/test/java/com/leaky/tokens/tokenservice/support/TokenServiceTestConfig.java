package com.leaky.tokens.tokenservice.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.leaky.tokens.tokenservice.bucket.InMemoryTokenBucketStore;
import com.leaky.tokens.tokenservice.bucket.TokenBucketStore;
import com.leaky.tokens.tokenservice.events.NoopTokenUsagePublisher;
import com.leaky.tokens.tokenservice.events.TokenUsagePublisher;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import javax.sql.DataSource;

@Configuration
public class TokenServiceTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Bean
    @ConditionalOnMissingBean(TokenBucketStore.class)
    public TokenBucketStore tokenBucketStore() {
        return new InMemoryTokenBucketStore();
    }

    @Bean
    @ConditionalOnMissingBean(TokenUsagePublisher.class)
    public TokenUsagePublisher tokenUsagePublisher() {
        return new NoopTokenUsagePublisher();
    }

    @Bean
    @ConditionalOnMissingBean(WebClient.Builder.class)
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "test")
            .claim("roles", java.util.List.of("USER"))
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(Flyway.class)
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
    }
}
