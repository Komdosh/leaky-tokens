package com.leaky.tokens.tokenservice.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxEntry;
import com.leaky.tokens.tokenservice.outbox.TokenOutboxRepository;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
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

        TokenPurchaseResponse response = service.start(request);
        assertThat(response.getStatus()).isEqualTo(TokenPurchaseSagaStatus.FAILED);

        ArgumentCaptor<TokenOutboxEntry> outboxCaptor = ArgumentCaptor.forClass(TokenOutboxEntry.class);
        verify(outboxRepository, times(4)).save(outboxCaptor.capture());

        List<String> eventTypes = outboxCaptor.getAllValues().stream()
            .map(TokenOutboxEntry::getEventType)
            .toList();

        assertThat(eventTypes).contains("TOKEN_PURCHASE_FAILED", "PAYMENT_RELEASE_REQUESTED");
        assertThat(eventTypes).doesNotContain("TOKEN_PURCHASE_COMPLETED");

        verify(quotaService, times(0)).addTokens(any(), any(), anyLong());
    }
}
