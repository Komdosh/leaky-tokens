package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import com.leaky.tokens.tokenservice.flags.TokenServiceFeatureFlags;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TokenPurchaseSagaService {
    private final TokenPurchaseSagaRepository sagaRepository;
    private final TokenOutboxRepository outboxRepository;
    private final TokenQuotaService quotaService;
    private final ObjectMapper objectMapper;
    private final boolean simulateFailure;
    private final TokenServiceFeatureFlags featureFlags;

    public TokenPurchaseSagaService(TokenPurchaseSagaRepository sagaRepository,
                                   TokenOutboxRepository outboxRepository,
                                   TokenQuotaService quotaService,
                                   ObjectMapper objectMapper,
                                   @Value("${token.saga.simulate-failure:false}") boolean simulateFailure,
                                   TokenServiceFeatureFlags featureFlags) {
        this.sagaRepository = sagaRepository;
        this.outboxRepository = outboxRepository;
        this.quotaService = quotaService;
        this.objectMapper = objectMapper;
        this.simulateFailure = simulateFailure;
        this.featureFlags = featureFlags;
    }

    @Transactional
    public TokenPurchaseResponse start(TokenPurchaseRequest request, TokenTierProperties.TierConfig tier, String idempotencyKey) {
        if (!featureFlags.isSagaPurchases()) {
            throw new IllegalStateException("Token purchase saga disabled by feature flag");
        }
        UUID userId = UUID.fromString(request.getUserId());
        UUID orgId = request.getOrgId() == null || request.getOrgId().isBlank()
            ? null
            : UUID.fromString(request.getOrgId());
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        if (normalizedKey != null) {
            Optional<TokenPurchaseSaga> existing = sagaRepository.findByIdempotencyKey(normalizedKey);
            if (existing.isPresent()) {
                TokenPurchaseSaga saga = existing.get();
                if (!matchesRequest(saga, userId, orgId, request)) {
                    throw new IdempotencyConflictException("Idempotency key reuse with different payload");
                }
                return new TokenPurchaseResponse(saga.getId(), saga.getStatus(), saga.getCreatedAt());
            }
        }
        TokenPurchaseSaga saga = new TokenPurchaseSaga(UUID.randomUUID(), userId, orgId,
            request.getProvider(), request.getTokens(), TokenPurchaseSagaStatus.STARTED);
        saga.setIdempotencyKey(normalizedKey);
        try {
            sagaRepository.save(saga);
        } catch (DataIntegrityViolationException ex) {
            if (normalizedKey != null) {
                TokenPurchaseSaga existing = sagaRepository.findByIdempotencyKey(normalizedKey)
                    .orElseThrow(() -> ex);
                if (!matchesRequest(existing, userId, orgId, request)) {
                    throw new IdempotencyConflictException("Idempotency key reuse with different payload");
                }
                return new TokenPurchaseResponse(existing.getId(), existing.getStatus(), existing.getCreatedAt());
            }
            throw ex;
        }
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

        try {
            if (orgId == null) {
                quotaService.addTokens(userId, request.getProvider(), request.getTokens(), tier);
            } else {
                quotaService.addOrgTokens(orgId, request.getProvider(), request.getTokens(), tier);
            }
        } catch (Exception ex) {
            saga.setStatus(TokenPurchaseSagaStatus.FAILED);
            sagaRepository.save(saga);
            emitEvent(saga, "TOKEN_PURCHASE_FAILED");
            emitCompensation(saga, "PAYMENT_RELEASE_REQUESTED");
            return new TokenPurchaseResponse(saga.getId(), saga.getStatus(), saga.getCreatedAt());
        }

        saga.setStatus(TokenPurchaseSagaStatus.TOKENS_ALLOCATED);
        sagaRepository.save(saga);
        emitEvent(saga, "TOKEN_ALLOCATED");

        saga.setStatus(TokenPurchaseSagaStatus.COMPLETED);
        sagaRepository.save(saga);
        emitEvent(saga, "TOKEN_PURCHASE_COMPLETED");

        return new TokenPurchaseResponse(saga.getId(), saga.getStatus(), saga.getCreatedAt());
    }

    private boolean matchesRequest(TokenPurchaseSaga saga, UUID userId, UUID orgId, TokenPurchaseRequest request) {
        if (!saga.getUserId().equals(userId)) {
            return false;
        }
        if (saga.getOrgId() == null && orgId != null) {
            return false;
        }
        if (saga.getOrgId() != null && !saga.getOrgId().equals(orgId)) {
            return false;
        }
        if (!saga.getProvider().equals(request.getProvider())) {
            return false;
        }
        return saga.getTokens() == request.getTokens();
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.isBlank() ? null : trimmed;
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
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize saga", e);
        }
    }
}
