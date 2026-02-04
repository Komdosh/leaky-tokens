package com.leaky.tokens.tokenservice.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenUsagePublisherConfig {
    @Bean
    @ConditionalOnMissingBean(TokenUsagePublisher.class)
    public TokenUsagePublisher tokenUsagePublisher() {
        return new NoopTokenUsagePublisher();
    }
}
