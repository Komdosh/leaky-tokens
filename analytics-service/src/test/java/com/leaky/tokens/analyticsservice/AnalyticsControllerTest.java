package com.leaky.tokens.analyticsservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.leaky.tokens.analyticsservice.metrics.AnalyticsMetrics;
import com.leaky.tokens.analyticsservice.report.AnalyticsAnomalyResponse;
import com.leaky.tokens.analyticsservice.report.AnalyticsReportResponse;
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

    @Test
    void reportReturnsServiceResponse() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportService reportService = Mockito.mock(AnalyticsReportService.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            reportService
        );

        AnalyticsReportResponse report = new AnalyticsReportResponse(
            "openai",
            Instant.parse("2026-02-04T16:00:00Z"),
            Instant.parse("2026-02-04T17:00:00Z"),
            10,
            8,
            2,
            1000,
            100.0,
            5,
            200,
            List.of(new com.leaky.tokens.analyticsservice.report.UserUsageSummary("user-1", 400, 4))
        );
        when(reportService.buildReport(eq("openai"), eq(60), eq(200))).thenReturn(report);

        AnalyticsReportResponse response = controller.report("openai", 60, 200);

        verify(reportService).buildReport(eq("openai"), eq(60), eq(200));
        assertThat(response.getProvider()).isEqualTo("openai");
        assertThat(response.getTotalEvents()).isEqualTo(10);
        assertThat(response.getTopUsers()).hasSize(1);
    }

    @Test
    void anomaliesReturnsServiceResponse() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportService reportService = Mockito.mock(AnalyticsReportService.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            reportService
        );

        AnalyticsAnomalyResponse anomaly = new AnalyticsAnomalyResponse(
            "openai",
            Instant.parse("2026-02-04T16:00:00Z"),
            Instant.parse("2026-02-04T17:00:00Z"),
            4,
            1200,
            300.0,
            4.0,
            2.0,
            true,
            200
        );
        when(reportService.detectAnomaly(eq("openai"), eq(60), eq(4), eq(2.0), eq(200))).thenReturn(anomaly);

        AnalyticsAnomalyResponse response = controller.anomalies("openai", 60, 4, 2.0, 200);

        verify(reportService).detectAnomaly(eq("openai"), eq(60), eq(4), eq(2.0), eq(200));
        assertThat(response.isAnomaly()).isTrue();
        assertThat(response.getCurrentTokens()).isEqualTo(1200);
    }

    @Test
    void reportPassesNullDefaultsToService() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportService reportService = Mockito.mock(AnalyticsReportService.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            reportService
        );

        AnalyticsReportResponse report = new AnalyticsReportResponse(
            "openai",
            Instant.parse("2026-02-04T16:00:00Z"),
            Instant.parse("2026-02-04T17:00:00Z"),
            0,
            0,
            0,
            0,
            0.0,
            0,
            0,
            List.of()
        );
        when(reportService.buildReport(eq("openai"), eq(null), eq(null))).thenReturn(report);

        AnalyticsReportResponse response = controller.report("openai", null, null);

        verify(reportService).buildReport(eq("openai"), eq(null), eq(null));
        assertThat(response.getTotalEvents()).isEqualTo(0);
    }

    @Test
    void anomaliesPassesNullDefaultsToService() {
        TokenUsageByProviderRepository repository = Mockito.mock(TokenUsageByProviderRepository.class);
        AnalyticsReportService reportService = Mockito.mock(AnalyticsReportService.class);
        AnalyticsController controller = new AnalyticsController(
            repository,
            new AnalyticsMetrics(new SimpleMeterRegistry()),
            reportService
        );

        AnalyticsAnomalyResponse anomaly = new AnalyticsAnomalyResponse(
            "openai",
            Instant.parse("2026-02-04T16:00:00Z"),
            Instant.parse("2026-02-04T17:00:00Z"),
            1,
            0,
            0.0,
            0.0,
            2.0,
            false,
            0
        );
        when(reportService.detectAnomaly(eq("openai"), eq(null), eq(null), eq(null), eq(null))).thenReturn(anomaly);

        AnalyticsAnomalyResponse response = controller.anomalies("openai", null, null, null, null);

        verify(reportService).detectAnomaly(eq("openai"), eq(null), eq(null), eq(null), eq(null));
        assertThat(response.isAnomaly()).isFalse();
    }
}
