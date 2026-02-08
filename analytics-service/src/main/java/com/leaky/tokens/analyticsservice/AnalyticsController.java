package com.leaky.tokens.analyticsservice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.leaky.tokens.analyticsservice.metrics.AnalyticsMetrics;
import com.leaky.tokens.analyticsservice.report.AnalyticsAnomalyResponse;
import com.leaky.tokens.analyticsservice.report.AnalyticsReportResponse;
import com.leaky.tokens.analyticsservice.report.AnalyticsReportService;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final TokenUsageByProviderRepository byProviderRepository;
    private final AnalyticsMetrics metrics;
    private final AnalyticsReportService reportService;

    @GetMapping("/api/v1/analytics/health")
    @Operation(
        summary = "Service health",
        responses = @ApiResponse(responseCode = "200", description = "Service healthy")
    )
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "analytics-service");
        response.put("status", "ok");
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    @GetMapping("/api/v1/analytics/usage")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Recent usage by provider",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Usage returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public Map<String, Object> usage(
        @RequestParam(name = "provider") String provider,
        @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<TokenUsageByProviderRecord> records = byProviderRepository.findRecentByProvider(provider, safeLimit);
        metrics.usageQuery(provider, "success");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", provider);
        response.put("count", records.size());
        response.put("items", records);
        return response;
    }

    @GetMapping("/api/v1/analytics/report")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Usage report for a window",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Report returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public AnalyticsReportResponse report(
        @RequestParam(name = "provider") String provider,
        @RequestParam(name = "windowMinutes", required = false) Integer windowMinutes,
        @RequestParam(name = "limit", required = false) Integer limit
    ) {
        AnalyticsReportResponse response = reportService.buildReport(provider, windowMinutes, limit);
        metrics.reportQuery(provider, "success");
        return response;
    }

    @GetMapping("/api/v1/analytics/anomalies")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Anomaly detection for token usage",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Anomaly analysis returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public AnalyticsAnomalyResponse anomalies(
        @RequestParam(name = "provider") String provider,
        @RequestParam(name = "windowMinutes", required = false) Integer windowMinutes,
        @RequestParam(name = "baselineWindows", required = false) Integer baselineWindows,
        @RequestParam(name = "thresholdMultiplier", required = false) Double thresholdMultiplier,
        @RequestParam(name = "limit", required = false) Integer limit
    ) {
        AnalyticsAnomalyResponse response = reportService.detectAnomaly(
            provider,
            windowMinutes,
            baselineWindows,
            thresholdMultiplier,
            limit
        );
        metrics.anomalyQuery(provider, "success");
        return response;
    }
}
