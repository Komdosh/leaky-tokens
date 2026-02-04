package com.leaky.tokens.analyticsservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.leaky.tokens.analyticsservice.metrics.AnalyticsMetrics;
import com.leaky.tokens.analyticsservice.report.AnalyticsReportService;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderKey;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AnalyticsControllerTest {
    @Test
    void healthReturnsStatus() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            Mockito.mock(AnalyticsReportService.class)
        );

        Map<String, Object> response = controller.health();

        assertThat(response.get("status")).isEqualTo("ok");
        assertThat(response.get("service")).isEqualTo("analytics-service");
    }

    @Test
    void usageClampsLimitAndReturnsRecords() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            Mockito.mock(AnalyticsReportService.class)
        );

        TokenUsageByProviderKey key = new TokenUsageByProviderKey("openai", Instant.now());
        TokenUsageByProviderRecord record = new TokenUsageByProviderRecord(key, "user-1", 42, true);
        when(repository.findRecentByProvider(eq("openai"), eq(200))).thenReturn(List.of(record));

        Map<String, Object> response = controller.usage("openai", 500);

        verify(repository).findRecentByProvider(eq("openai"), eq(200));
        assertThat(response.get("provider")).isEqualTo("openai");
        assertThat(response.get("count")).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<TokenUsageByProviderRecord> items = (List<TokenUsageByProviderRecord>) response.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getUserId()).isEqualTo("user-1");
    }

    @Test
    void usageClampsLowLimitToOne() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            Mockito.mock(AnalyticsReportService.class)
        );

        when(repository.findRecentByProvider(eq("openai"), eq(1))).thenReturn(List.of());

        Map<String, Object> response = controller.usage("openai", 0);

        verify(repository).findRecentByProvider(eq("openai"), eq(1));
        assertThat(response.get("count")).isEqualTo(0);
    }
}
