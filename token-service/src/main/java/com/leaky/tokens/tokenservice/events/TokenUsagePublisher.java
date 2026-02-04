package com.leaky.tokens.tokenservice.events;

public interface TokenUsagePublisher {
    void publish(TokenUsageEvent event);
}
