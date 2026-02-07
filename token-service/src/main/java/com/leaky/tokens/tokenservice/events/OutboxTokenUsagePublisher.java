package com.leaky.tokens.tokenservice.events;

import java.time.Instant;
import java.util.UUID;

import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(TokenOutboxRepository.class)
public class OutboxTokenUsagePublisher implements TokenUsagePublisher {
    private final TokenOutboxRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(TokenUsageEvent event) {
        String payload = toJson(event);
        TokenOutboxEntry entry = new TokenOutboxEntry(
            UUID.randomUUID(),
            "TokenUsage",
            null,
            "TOKEN_USAGE",
            payload,
            Instant.now(),
            null
        );
        repository.save(entry);
    }

    private String toJson(TokenUsageEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize token usage event", e);
        }
    }
}
