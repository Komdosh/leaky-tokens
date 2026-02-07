package com.leaky.tokens.tokenservice.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {
    private static final String TOPIC = "token-usage";

    @Mock
    private TokenOutboxRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void publishBatchDoesNothingWhenNoEntries() {
        OutboxPublisherJob job = new OutboxPublisherJob(repository, kafkaTemplate, TOPIC, 25);
        when(repository.findUnpublished(any(Pageable.class))).thenReturn(List.of());

        job.publishBatch();

        verify(repository).findUnpublished(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(25);
        verifyNoInteractions(kafkaTemplate);
        verify(repository, never()).save(any(TokenOutboxEntry.class));
    }

    @Test
    void publishBatchSendsAndMarksPublished() {
        OutboxPublisherJob job = new OutboxPublisherJob(repository, kafkaTemplate, TOPIC, 10);
        TokenOutboxEntry first = entry("TOKEN_PURCHASE_COMPLETED");
        TokenOutboxEntry second = entry("TOKEN_ALLOCATED");
        when(repository.findUnpublished(any(Pageable.class))).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(eq(TOPIC), eq(first.getId().toString()), eq(first.getPayload())))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaTemplate.send(eq(TOPIC), eq(second.getId().toString()), eq(second.getPayload())))
            .thenReturn(CompletableFuture.completedFuture(null));

        job.publishBatch();

        assertThat(first.getPublishedAt()).isNotNull();
        assertThat(second.getPublishedAt()).isNotNull();
        verify(repository, times(2)).save(any(TokenOutboxEntry.class));
        verify(kafkaTemplate, times(2)).send(eq(TOPIC), any(String.class), any(String.class));
    }

    @Test
    void publishBatchStopsOnFailure() {
        OutboxPublisherJob job = new OutboxPublisherJob(repository, kafkaTemplate, TOPIC, 10);
        TokenOutboxEntry first = entry("TOKEN_PURCHASE_COMPLETED");
        TokenOutboxEntry second = entry("TOKEN_ALLOCATED");
        CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka down"));
        when(repository.findUnpublished(any(Pageable.class))).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(eq(TOPIC), eq(first.getId().toString()), eq(first.getPayload())))
            .thenReturn(failed);

        job.publishBatch();

        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(repository, never()).save(any(TokenOutboxEntry.class));
        verify(kafkaTemplate).send(eq(TOPIC), eq(first.getId().toString()), eq(first.getPayload()));
        verifyNoMoreInteractions(kafkaTemplate);
    }

    private static TokenOutboxEntry entry(String eventType) {
        return new TokenOutboxEntry(
            UUID.randomUUID(),
            "TOKEN_PURCHASE",
            UUID.randomUUID(),
            eventType,
            "{\"event\":\"" + eventType + "\"}",
            Instant.now(),
            null
        );
    }
}
