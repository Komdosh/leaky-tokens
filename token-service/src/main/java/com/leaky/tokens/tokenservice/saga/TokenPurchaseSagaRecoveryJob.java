package com.leaky.tokens.tokenservice.saga;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class TokenPurchaseSagaRecoveryJob {
    private final TokenPurchaseSagaRepository sagaRepository;
    private final TokenOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration staleDuration;

    public TokenPurchaseSagaRecoveryJob(TokenPurchaseSagaRepository sagaRepository,
                                        TokenOutboxRepository outboxRepository,
                                        ObjectMapper objectMapper,
                                        @Value("${token.saga.recovery.enabled:true}") boolean enabled,
                                        @Value("${token.saga.recovery.stale-minutes:10}") long staleMinutes) {
        this.sagaRepository = sagaRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.staleDuration = Duration.ofMinutes(staleMinutes);
    }

    @Scheduled(fixedDelayString = "${token.saga.recovery.fixed-delay-ms:60000}")
    @Transactional
    public void recoverStaleSagas() {
        if (!enabled) {
            return;
        }
        Instant cutoff = Instant.now().minus(staleDuration);
        List<TokenPurchaseSaga> sagas = sagaRepository.findByStatusInAndUpdatedAtBefore(
            List.of(TokenPurchaseSagaStatus.STARTED, TokenPurchaseSagaStatus.PAYMENT_RESERVED, TokenPurchaseSagaStatus.TOKENS_ALLOCATED),
            cutoff
        );
        for (TokenPurchaseSaga saga : sagas) {
            try {
                if (saga.getStatus() == TokenPurchaseSagaStatus.TOKENS_ALLOCATED) {
                    saga.setStatus(TokenPurchaseSagaStatus.COMPLETED);
                    sagaRepository.save(saga);
                    emitEvent(saga, "TOKEN_PURCHASE_COMPLETED");
                } else {
                    saga.setStatus(TokenPurchaseSagaStatus.FAILED);
                    sagaRepository.save(saga);
                    emitEvent(saga, "TOKEN_PURCHASE_FAILED");
                    emitCompensation(saga, "PAYMENT_RELEASE_REQUESTED");
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void emitEvent(TokenPurchaseSaga saga, String eventType) {
        TokenOutboxEntry entry = new TokenOutboxEntry(
            UUID.randomUUID(),
            "TokenPurchaseSagaRecovery",
            saga.getId(),
            eventType,
            toJson(saga),
            Instant.now(),
            null
        );
        outboxRepository.save(entry);
    }

    private void emitCompensation(TokenPurchaseSaga saga, String eventType) {
        TokenOutboxEntry entry = new TokenOutboxEntry(
            UUID.randomUUID(),
            "TokenPurchaseSagaRecoveryCompensation",
            saga.getId(),
            eventType,
            toJson(saga),
            Instant.now(),
            null
        );
        outboxRepository.save(entry);
    }

    private String toJson(TokenPurchaseSaga saga) {
        try {
            return objectMapper.writeValueAsString(saga);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize saga", e);
        }
    }
}
