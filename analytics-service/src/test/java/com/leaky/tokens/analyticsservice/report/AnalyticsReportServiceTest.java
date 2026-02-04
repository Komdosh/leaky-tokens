package com.leaky.tokens.analyticsservice.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderKey;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AnalyticsReportServiceTest {
    @Test
    void buildReportSummarizesUsageAndTopUsers() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportProperties properties = new AnalyticsReportProperties();
        properties.setDefaultWindowMinutes(60);
        properties.setMaxWindowMinutes(120);
        properties.setMaxLimit(100);
        properties.setMaxTopUsers(1);

        when(repository.findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(100)))
            .thenReturn(List.of(
                record("openai", "user-a", 100, true),
                record("openai", "user-a", 50, false),
                record("openai", "user-b", 25, true)
            ));

        AnalyticsReportService service = new AnalyticsReportService(repository, properties);
        AnalyticsReportResponse response = service.buildReport("openai", null, null);

        assertThat(response.getProvider()).isEqualTo("openai");
        assertThat(Duration.between(response.getWindowStart(), response.getWindowEnd()).toMinutes()).isBetween(59L, 61L);
        assertThat(response.getTotalEvents()).isEqualTo(3);
        assertThat(response.getAllowedEvents()).isEqualTo(2);
        assertThat(response.getDeniedEvents()).isEqualTo(1);
        assertThat(response.getTotalTokens()).isEqualTo(175);
        assertThat(response.getAverageTokensPerEvent()).isCloseTo(58.333, org.assertj.core.data.Offset.offset(0.01));
        assertThat(response.getUniqueUsers()).isEqualTo(2);
        assertThat(response.getSampleLimit()).isEqualTo(100);
        assertThat(response.getTopUsers()).hasSize(1);
        assertThat(response.getTopUsers().get(0).getUserId()).isEqualTo("user-a");
        assertThat(response.getTopUsers().get(0).getTotalTokens()).isEqualTo(150);
        assertThat(response.getTopUsers().get(0).getEvents()).isEqualTo(2);
    }

    @Test
    void detectAnomalyFlagsAboveThreshold() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportProperties properties = new AnalyticsReportProperties();
        properties.setDefaultWindowMinutes(60);
        properties.setMaxWindowMinutes(120);
        properties.setMaxLimit(100);
        properties.setDefaultBaselineWindows(2);
        properties.setMaxBaselineWindows(4);
        properties.setDefaultAnomalyThresholdMultiplier(2.0);

        when(repository.findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(100)))
            .thenReturn(List.of(record("openai", "user-a", 200, true), record("openai", "user-b", 100, true)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)))
            .thenReturn(List.of(record("openai", "user-b", 100, true)));

        AnalyticsReportService service = new AnalyticsReportService(repository, properties);
        AnalyticsAnomalyResponse response = service.detectAnomaly("openai", 60, 2, 2.0, 100);

        assertThat(response.isAnomaly()).isTrue();
        assertThat(response.getCurrentTokens()).isEqualTo(300);
        assertThat(response.getBaselineAverageTokens()).isEqualTo(100.0);
        assertThat(response.getRatio()).isEqualTo(3.0);
        assertThat(response.getThresholdMultiplier()).isEqualTo(2.0);
    }

    @Test
    void detectAnomalyHandlesZeroBaseline() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportProperties properties = new AnalyticsReportProperties();
        properties.setDefaultWindowMinutes(60);
        properties.setMaxWindowMinutes(120);
        properties.setMaxLimit(100);
        properties.setDefaultBaselineWindows(1);
        properties.setMaxBaselineWindows(4);

        when(repository.findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(100)))
            .thenReturn(List.of(record("openai", "user-a", 50, true)))
            .thenReturn(List.of());

        AnalyticsReportService service = new AnalyticsReportService(repository, properties);
        AnalyticsAnomalyResponse response = service.detectAnomaly("openai", 60, 1, null, 100);

        assertThat(response.getBaselineAverageTokens()).isEqualTo(0.0);
        assertThat(response.getRatio()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(response.isAnomaly()).isFalse();
    }

    @Test
    void detectAnomalyClampsThresholdMultiplier() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportProperties properties = new AnalyticsReportProperties();
        properties.setDefaultWindowMinutes(60);
        properties.setMaxWindowMinutes(120);
        properties.setMaxLimit(100);
        properties.setDefaultBaselineWindows(1);
        properties.setMaxBaselineWindows(4);

        when(repository.findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(100)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)));

        AnalyticsReportService service = new AnalyticsReportService(repository, properties);
        AnalyticsAnomalyResponse response = service.detectAnomaly("openai", 60, 1, 0.2, 100);

        assertThat(response.getThresholdMultiplier()).isEqualTo(1.0);
    }

    @Test
    void buildReportClampsWindowAndUsesMaxLimit() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportProperties properties = new AnalyticsReportProperties();
        properties.setDefaultWindowMinutes(60);
        properties.setMaxWindowMinutes(120);
        properties.setMaxLimit(50);

        when(repository.findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(50)))
            .thenReturn(List.of());

        AnalyticsReportService service = new AnalyticsReportService(repository, properties);
        AnalyticsReportResponse response = service.buildReport("openai", 500, null);

        assertThat(response.getSampleLimit()).isEqualTo(50);
        verify(repository).findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(50));
    }

    @Test
    void detectAnomalyClampsBaselineWindowsToMax() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportProperties properties = new AnalyticsReportProperties();
        properties.setDefaultWindowMinutes(60);
        properties.setMaxWindowMinutes(120);
        properties.setMaxLimit(100);
        properties.setDefaultBaselineWindows(2);
        properties.setMaxBaselineWindows(4);

        when(repository.findByProviderAndTimestampRange(eq("openai"), any(), any(), eq(100)))
            .thenReturn(List.of(record("openai", "user-a", 200, true)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)))
            .thenReturn(List.of(record("openai", "user-a", 100, true)));

        AnalyticsReportService service = new AnalyticsReportService(repository, properties);
        AnalyticsAnomalyResponse response = service.detectAnomaly("openai", 60, 99, 2.0, 100);

        assertThat(response.getBaselineWindows()).isEqualTo(4);
    }

    private static TokenUsageByProviderRecord record(String provider, String userId, long tokens, boolean allowed) {
        TokenUsageByProviderKey key = new TokenUsageByProviderKey(provider, Instant.now());
        return new TokenUsageByProviderRecord(key, userId, tokens, allowed);
    }
}
