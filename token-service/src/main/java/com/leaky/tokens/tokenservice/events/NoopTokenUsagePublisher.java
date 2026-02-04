package com.leaky.tokens.tokenservice.events;

public class NoopTokenUsagePublisher implements TokenUsagePublisher {
    @Override
    public void publish(TokenUsageEvent event) {
        // no-op
    }
}
