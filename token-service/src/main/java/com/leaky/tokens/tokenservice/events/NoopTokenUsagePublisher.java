package com.leaky.tokens.tokenservice.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(TokenUsagePublisher.class)
public class NoopTokenUsagePublisher implements TokenUsagePublisher {
    @Override
    public void publish(TokenUsageEvent event) {
        // no-op
    }
}
