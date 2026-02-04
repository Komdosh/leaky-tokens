package com.leaky.tokens.tokenservice.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
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
            true
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
            false
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
}
