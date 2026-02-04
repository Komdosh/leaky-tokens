package com.leaky.tokens.analyticsservice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.leaky.tokens.analyticsservice.metrics.AnalyticsMetrics;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {
    private final TokenUsageByProviderRepository byProviderRepository;
    private final AnalyticsMetrics metrics;

    public AnalyticsController(TokenUsageByProviderRepository byProviderRepository, AnalyticsMetrics metrics) {
        this.byProviderRepository = byProviderRepository;
        this.metrics = metrics;
    }

    @GetMapping("/api/v1/analytics/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "analytics-service");
        response.put("status", "ok");
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    @GetMapping("/api/v1/analytics/usage")
    @PreAuthorize("hasRole('USER')")
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
}
