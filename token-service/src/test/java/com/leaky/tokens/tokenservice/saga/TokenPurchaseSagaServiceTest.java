package com.leaky.tokens.tokenservice.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import com.leaky.tokens.tokenservice.flags.TokenServiceFeatureFlags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenPurchaseSagaServiceTest {
    @Mock
    private TokenPurchaseSagaRepository sagaRepository;

    @Mock
    private TokenOutboxRepository outboxRepository;

    @Mock
    private TokenQuotaService quotaService;

    @Test
    void emitsCompensationOnFailure() {
        when(sagaRepository.save(any(TokenPurchaseSaga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any(TokenOutboxEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPurchaseSagaService service = new TokenPurchaseSagaService(
            sagaRepository,
            outboxRepository,
            quotaService,
            new ObjectMapper(),
            true,
            enabledFlags()
        );

        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("openai");
        request.setTokens(10);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        TokenPurchaseResponse response = service.start(request, tier, null);
        assertThat(response.getStatus()).isEqualTo(TokenPurchaseSagaStatus.FAILED);

        ArgumentCaptor<TokenOutboxEntry> outboxCaptor = ArgumentCaptor.forClass(TokenOutboxEntry.class);
        verify(outboxRepository, times(4)).save(outboxCaptor.capture());

        List<String> eventTypes = outboxCaptor.getAllValues().stream()
            .map(TokenOutboxEntry::getEventType)
            .toList();

        assertThat(eventTypes).contains("TOKEN_PURCHASE_FAILED", "PAYMENT_RELEASE_REQUESTED");
        assertThat(eventTypes).doesNotContain("TOKEN_PURCHASE_COMPLETED");

        verify(quotaService, times(0)).addTokens(any(), any(), anyLong(), any());
    }

    @Test
    void returnsExistingSagaForIdempotencyKey() {
        TokenPurchaseSaga existing = new TokenPurchaseSaga(
            java.util.UUID.randomUUID(),
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            null,
            "openai",
            10,
            TokenPurchaseSagaStatus.COMPLETED
        );
        existing.setIdempotencyKey("idem-1");

        when(sagaRepository.findByIdempotencyKey(eq("idem-1"))).thenReturn(Optional.of(existing));

        TokenPurchaseSagaService service = new TokenPurchaseSagaService(
            sagaRepository,
            outboxRepository,
            quotaService,
            new ObjectMapper(),
            false,
            enabledFlags()
        );

        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("openai");
        request.setTokens(10);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        TokenPurchaseResponse response = service.start(request, tier, "idem-1");

        assertThat(response.getStatus()).isEqualTo(TokenPurchaseSagaStatus.COMPLETED);
        verify(quotaService, times(0)).addTokens(any(), any(), anyLong(), any());
        verify(outboxRepository, times(0)).save(any(TokenOutboxEntry.class));
    }

    @Test
    void throwsConflictWhenIdempotencyKeyReusedWithDifferentPayload() {
        TokenPurchaseSaga existing = new TokenPurchaseSaga(
            java.util.UUID.randomUUID(),
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            null,
            "openai",
            10,
            TokenPurchaseSagaStatus.STARTED
        );
        existing.setIdempotencyKey("idem-1");

        when(sagaRepository.findByIdempotencyKey(eq("idem-1"))).thenReturn(Optional.of(existing));

        TokenPurchaseSagaService service = new TokenPurchaseSagaService(
            sagaRepository,
            outboxRepository,
            quotaService,
            new ObjectMapper(),
            false,
            enabledFlags()
        );

        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("gemini");
        request.setTokens(10);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(request, tier, "idem-1"))
            .isInstanceOf(IdempotencyConflictException.class)
            .hasMessageContaining("Idempotency key reuse");
    }

    @Test
    void skipsSagaWhenFeatureFlagDisabled() {
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setSagaPurchases(false);
        flags.setQuotaEnforcement(true);

        TokenPurchaseSagaService service = new TokenPurchaseSagaService(
            sagaRepository,
            outboxRepository,
            quotaService,
            new ObjectMapper(),
            false,
            flags
        );

        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("openai");
        request.setTokens(10);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(request, tier, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("saga disabled");
    }

    @Test
    void marksSagaFailedWhenQuotaAllocationThrows() {
        when(sagaRepository.save(any(TokenPurchaseSaga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any(TokenOutboxEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("quota down"))
            .when(quotaService).addTokens(any(), any(), anyLong(), any());

        TokenPurchaseSagaService service = new TokenPurchaseSagaService(
            sagaRepository,
            outboxRepository,
            quotaService,
            new ObjectMapper(),
            false,
            enabledFlags()
        );

        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("openai");
        request.setTokens(10);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        TokenPurchaseResponse response = service.start(request, tier, null);

        assertThat(response.getStatus()).isEqualTo(TokenPurchaseSagaStatus.FAILED);

        ArgumentCaptor<TokenOutboxEntry> outboxCaptor = ArgumentCaptor.forClass(TokenOutboxEntry.class);
        verify(outboxRepository, times(4)).save(outboxCaptor.capture());
        List<String> eventTypes = outboxCaptor.getAllValues().stream()
            .map(TokenOutboxEntry::getEventType)
            .toList();
        assertThat(eventTypes).contains("TOKEN_PURCHASE_FAILED", "PAYMENT_RELEASE_REQUESTED");
    }

    @Test
    void recoveryMarksStaleSagas() {
        TokenPurchaseSaga started = new TokenPurchaseSaga(
            UUID.randomUUID(),
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            null,
            "openai",
            5,
            TokenPurchaseSagaStatus.STARTED
        );
        TokenPurchaseSaga paymentReserved = new TokenPurchaseSaga(
            UUID.randomUUID(),
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            null,
            "openai",
            5,
            TokenPurchaseSagaStatus.PAYMENT_RESERVED
        );
        TokenPurchaseSaga allocated = new TokenPurchaseSaga(
            UUID.randomUUID(),
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            null,
            "openai",
            5,
            TokenPurchaseSagaStatus.TOKENS_ALLOCATED
        );

        when(sagaRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
            .thenReturn(List.of(started, paymentReserved, allocated));
        when(sagaRepository.save(any(TokenPurchaseSaga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any(TokenOutboxEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPurchaseSagaRecoveryJob job = new TokenPurchaseSagaRecoveryJob(
            sagaRepository,
            outboxRepository,
            new ObjectMapper(),
            true,
            10
        );

        job.recoverStaleSagas();

        assertThat(started.getStatus()).isEqualTo(TokenPurchaseSagaStatus.FAILED);
        assertThat(paymentReserved.getStatus()).isEqualTo(TokenPurchaseSagaStatus.FAILED);
        assertThat(allocated.getStatus()).isEqualTo(TokenPurchaseSagaStatus.COMPLETED);

        ArgumentCaptor<TokenOutboxEntry> outboxCaptor = ArgumentCaptor.forClass(TokenOutboxEntry.class);
        verify(outboxRepository, times(5)).save(outboxCaptor.capture());
        List<String> eventTypes = outboxCaptor.getAllValues().stream()
            .map(TokenOutboxEntry::getEventType)
            .toList();
        assertThat(eventTypes).contains("TOKEN_PURCHASE_FAILED", "PAYMENT_RELEASE_REQUESTED", "TOKEN_PURCHASE_COMPLETED");
    }

    @Test
    void recoverySkipsWhenDisabled() {
        TokenPurchaseSagaRecoveryJob job = new TokenPurchaseSagaRecoveryJob(
            sagaRepository,
            outboxRepository,
            new ObjectMapper(),
            false,
            10
        );

        job.recoverStaleSagas();

        verify(sagaRepository, times(0)).findByStatusInAndUpdatedAtBefore(any(), any());
        verify(outboxRepository, times(0)).save(any(TokenOutboxEntry.class));
    }

    @Test
    void ignoresBlankIdempotencyKey() {
        when(sagaRepository.save(any(TokenPurchaseSaga.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxRepository.save(any(TokenOutboxEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPurchaseSagaService service = new TokenPurchaseSagaService(
            sagaRepository,
            outboxRepository,
            quotaService,
            new ObjectMapper(),
            false,
            enabledFlags()
        );

        TokenPurchaseRequest request = new TokenPurchaseRequest();
        request.setUserId("00000000-0000-0000-0000-000000000001");
        request.setProvider("openai");
        request.setTokens(5);

        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        TokenPurchaseResponse response = service.start(request, tier, "   ");

        assertThat(response.getStatus()).isEqualTo(TokenPurchaseSagaStatus.COMPLETED);
        verify(sagaRepository, times(0)).findByIdempotencyKey(any());
        verify(outboxRepository, times(4)).save(any(TokenOutboxEntry.class));
    }

    private TokenServiceFeatureFlags enabledFlags() {
        TokenServiceFeatureFlags flags = new TokenServiceFeatureFlags();
        flags.setQuotaEnforcement(true);
        flags.setSagaPurchases(true);
        return flags;
    }
}
