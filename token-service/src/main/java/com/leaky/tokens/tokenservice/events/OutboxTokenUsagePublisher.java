package com.leaky.tokens.tokenservice.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(TokenOutboxRepository.class)
public class OutboxTokenUsagePublisher implements TokenUsagePublisher {
    private final TokenOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxTokenUsagePublisher(TokenOutboxRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize token usage event", e);
        }
    }
}
