package com.leaky.tokens.tokenservice.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxTokenUsagePublisherTest {
    @Mock
    private TokenOutboxRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<TokenOutboxEntry> entryCaptor;

    @Test
    void publishPersistsSerializedEvent() throws Exception {
        OutboxTokenUsagePublisher publisher = new OutboxTokenUsagePublisher(repository, objectMapper);
        TokenUsageEvent event = new TokenUsageEvent("user-1", "openai", 25, true, Instant.now());
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"ok\":true}");

        publisher.publish(event);

        verify(repository).save(entryCaptor.capture());
        TokenOutboxEntry entry = entryCaptor.getValue();
        assertThat(entry.getAggregateType()).isEqualTo("TokenUsage");
        assertThat(entry.getAggregateId()).isNull();
        assertThat(entry.getEventType()).isEqualTo("TOKEN_USAGE");
        assertThat(entry.getPayload()).isEqualTo("{\"ok\":true}");
        assertThat(entry.getPublishedAt()).isNull();
        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getCreatedAt()).isNotNull();
    }

    @Test
    void publishThrowsWhenSerializationFails() throws Exception {
        OutboxTokenUsagePublisher publisher = new OutboxTokenUsagePublisher(repository, objectMapper);
        TokenUsageEvent event = new TokenUsageEvent("user-1", "openai", 25, false, Instant.now());
        when(objectMapper.writeValueAsString(event))
            .thenThrow(new JacksonException("boom") { } );

        assertThatThrownBy(() -> publisher.publish(event))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Failed to serialize token usage event");
    }
}
