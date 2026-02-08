package com.leaky.tokens.analyticsservice.events;

import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderKey;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import com.leaky.tokens.analyticsservice.storage.TokenUsageRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenUsageListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TokenUsageRepository repository;

    @Mock
    private TokenUsageByProviderRepository byProviderRepository;

    @InjectMocks
    private TokenUsageListener listener;

    @Test
    void onmessage_persists_usage_and_by_provider_records() throws Exception {
        TokenUsageEvent event = new TokenUsageEvent();
        event.setUserId("00000000-0000-0000-0000-000000000001");
        event.setProvider("openai");
        event.setTokens(42L);
        event.setAllowed(true);
        event.setTimestamp(Instant.parse("2026-02-07T13:00:00Z"));

        String payload = "{\"userId\":\"00000000-0000-0000-0000-000000000001\"}";
        when(objectMapper.readValue(payload, TokenUsageEvent.class)).thenReturn(event);

        listener.onMessage(payload);

        ArgumentCaptor<TokenUsageRecord> recordCaptor = ArgumentCaptor.forClass(TokenUsageRecord.class);
        verify(repository).save(recordCaptor.capture());
        TokenUsageRecord record = recordCaptor.getValue();
        assertThat(record.getId()).isNotNull();
        assertThat(record.getUserId()).isEqualTo(event.getUserId());
        assertThat(record.getProvider()).isEqualTo(event.getProvider());
        assertThat(record.getTokens()).isEqualTo(event.getTokens());
        assertThat(record.isAllowed()).isEqualTo(event.isAllowed());
        assertThat(record.getTimestamp()).isEqualTo(event.getTimestamp());

        ArgumentCaptor<TokenUsageByProviderRecord> byProviderCaptor = ArgumentCaptor.forClass(TokenUsageByProviderRecord.class);
        verify(byProviderRepository).save(byProviderCaptor.capture());
        TokenUsageByProviderRecord byProvider = byProviderCaptor.getValue();
        TokenUsageByProviderKey key = byProvider.getKey();
        assertThat(key).isNotNull();
        assertThat(key.getProvider()).isEqualTo(event.getProvider());
        assertThat(key.getTimestamp()).isEqualTo(event.getTimestamp());
        assertThat(byProvider.getUserId()).isEqualTo(event.getUserId());
        assertThat(byProvider.getTokens()).isEqualTo(event.getTokens());
        assertThat(byProvider.isAllowed()).isEqualTo(event.isAllowed());
    }

    @Test
    void onmessage_does_not_persist_when_payload_is_invalid() throws Exception {
        String payload = "not-json";
        when(objectMapper.readValue(payload, TokenUsageEvent.class)).thenThrow(new RuntimeException("bad"));

        listener.onMessage(payload);

        verifyNoInteractions(repository, byProviderRepository);
    }

    @Test
    void onmessage_does_not_save_by_provider_when_primary_save_fails() throws Exception {
        TokenUsageEvent event = new TokenUsageEvent();
        event.setUserId("00000000-0000-0000-0000-000000000001");
        event.setProvider("openai");
        event.setTokens(7L);
        event.setAllowed(false);
        event.setTimestamp(Instant.parse("2026-02-07T13:10:00Z"));

        String payload = "{\"userId\":\"00000000-0000-0000-0000-000000000001\"}";
        when(objectMapper.readValue(payload, TokenUsageEvent.class)).thenReturn(event);
        doThrow(new RuntimeException("cassandra down")).when(repository).save(org.mockito.ArgumentMatchers.any(TokenUsageRecord.class));

        listener.onMessage(payload);

        verify(byProviderRepository, never()).save(org.mockito.ArgumentMatchers.any(TokenUsageByProviderRecord.class));
    }
}
