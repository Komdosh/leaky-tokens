package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenPurchaseSagaService {
    private final TokenPurchaseSagaRepository sagaRepository;
    private final TokenOutboxRepository outboxRepository;
    private final TokenQuotaService quotaService;
    private final ObjectMapper objectMapper;
    private final boolean simulateFailure;

    public TokenPurchaseSagaService(TokenPurchaseSagaRepository sagaRepository,
                                   TokenOutboxRepository outboxRepository,
                                   TokenQuotaService quotaService,
                                   ObjectMapper objectMapper,
                                   @Value("${token.saga.simulate-failure:false}") boolean simulateFailure) {
        this.sagaRepository = sagaRepository;
        this.outboxRepository = outboxRepository;
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
        this.simulateFailure = simulateFailure;
    }

    @Transactional
    public TokenPurchaseResponse start(TokenPurchaseRequest request) {
        UUID userId = UUID.fromString(request.getUserId());
        TokenPurchaseSaga saga = new TokenPurchaseSaga(UUID.randomUUID(), userId,
            request.getProvider(), request.getTokens(), TokenPurchaseSagaStatus.STARTED);
        sagaRepository.save(saga);
        emitEvent(saga, "TOKEN_PURCHASE_STARTED");

        saga.setStatus(TokenPurchaseSagaStatus.PAYMENT_RESERVED);
        sagaRepository.save(saga);
        emitEvent(saga, "TOKEN_PAYMENT_RESERVED");

        if (simulateFailure) {
            saga.setStatus(TokenPurchaseSagaStatus.FAILED);
            sagaRepository.save(saga);
            emitEvent(saga, "TOKEN_PURCHASE_FAILED");
            emitCompensation(saga, "PAYMENT_RELEASE_REQUESTED");
            return new TokenPurchaseResponse(saga.getId(), saga.getStatus(), saga.getCreatedAt());
        }

        quotaService.addTokens(userId, request.getProvider(), request.getTokens());
        saga.setStatus(TokenPurchaseSagaStatus.TOKENS_ALLOCATED);
        sagaRepository.save(saga);
        emitEvent(saga, "TOKEN_ALLOCATED");

        saga.setStatus(TokenPurchaseSagaStatus.COMPLETED);
        sagaRepository.save(saga);
        emitEvent(saga, "TOKEN_PURCHASE_COMPLETED");

        return new TokenPurchaseResponse(saga.getId(), saga.getStatus(), saga.getCreatedAt());
    }

    private void emitEvent(TokenPurchaseSaga saga, String eventType) {
        TokenOutboxEntry entry = new TokenOutboxEntry(
            UUID.randomUUID(),
            "TokenPurchaseSaga",
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
            "TokenPurchaseSagaCompensation",
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
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize saga", e);
        }
    }
}
