package com.leaky.tokens.analyticsservice.report;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRecord;
import com.leaky.tokens.analyticsservice.storage.TokenUsageByProviderRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsReportService {
    private final TokenUsageByProviderRepository byProviderRepository;
    private final AnalyticsReportProperties properties;

    public AnalyticsReportService(TokenUsageByProviderRepository byProviderRepository,
                                  AnalyticsReportProperties properties) {
        this.byProviderRepository = byProviderRepository;
        this.properties = properties;
    }

    public AnalyticsReportResponse buildReport(String provider, Integer windowMinutes, Integer limit) {
        int window = clamp(windowMinutes, properties.getDefaultWindowMinutes(), properties.getMaxWindowMinutes());
        int sampleLimit = clamp(limit, properties.getMaxLimit(), properties.getMaxLimit());
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(Duration.ofMinutes(window));

        List<TokenUsageByProviderRecord> records = byProviderRepository
            .findByProviderAndTimestampRange(provider, windowStart, windowEnd, sampleLimit);

        return summarize(provider, windowStart, windowEnd, sampleLimit, records);
    }

    public AnalyticsAnomalyResponse detectAnomaly(String provider,
                                                  Integer windowMinutes,
                                                  Integer baselineWindows,
                                                  Double thresholdMultiplier,
                                                  Integer limit) {
        int window = clamp(windowMinutes, properties.getDefaultWindowMinutes(), properties.getMaxWindowMinutes());
        int baseline = clamp(baselineWindows, properties.getDefaultBaselineWindows(), properties.getMaxBaselineWindows());
        double threshold = thresholdMultiplier == null ? properties.getDefaultAnomalyThresholdMultiplier()
            : Math.max(1.0, thresholdMultiplier);
        int sampleLimit = clamp(limit, properties.getMaxLimit(), properties.getMaxLimit());

        Instant now = Instant.now();
        Instant currentStart = now.minus(Duration.ofMinutes(window));
        long currentTokens = sumTokens(provider, currentStart, now, sampleLimit);

        double baselineSum = 0.0;
        for (int i = 1; i <= baseline; i++) {
            Instant end = now.minus(Duration.ofMinutes((long) window * i));
            Instant start = end.minus(Duration.ofMinutes(window));
            baselineSum += sumTokens(provider, start, end, sampleLimit);
        }

        double baselineAverage = baseline == 0 ? 0.0 : baselineSum / baseline;
        double ratio = baselineAverage == 0.0 ? (currentTokens > 0 ? Double.POSITIVE_INFINITY : 0.0)
            : currentTokens / baselineAverage;
        boolean anomaly = baselineAverage > 0.0 && ratio >= threshold;

        return new AnalyticsAnomalyResponse(
            provider,
            currentStart,
            now,
            baseline,
            currentTokens,
            baselineAverage,
            ratio,
            threshold,
            anomaly,
            sampleLimit
        );
    }

    private AnalyticsReportResponse summarize(String provider,
                                              Instant windowStart,
                                              Instant windowEnd,
                                              int sampleLimit,
                                              List<TokenUsageByProviderRecord> records) {
        int totalEvents = records.size();
        int allowedEvents = 0;
        long totalTokens = 0L;
        Set<String> users = new HashSet<>();
        Map<String, UserAggregate> aggregates = new HashMap<>();

        for (TokenUsageByProviderRecord record : records) {
            if (record.isAllowed()) {
                allowedEvents += 1;
            }
            totalTokens += record.getTokens();
            String userId = record.getUserId();
            if (userId != null) {
                users.add(userId);
                aggregates.computeIfAbsent(userId, key -> new UserAggregate())
                    .add(record.getTokens());
            }
        }

        int deniedEvents = totalEvents - allowedEvents;
        double averageTokens = totalEvents == 0 ? 0.0 : (double) totalTokens / totalEvents;

        List<UserUsageSummary> topUsers = new ArrayList<>();
        int topLimit = properties.getMaxTopUsers();
        aggregates.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(UserAggregate::totalTokens).reversed()))
            .limit(topLimit)
            .forEach(entry -> topUsers.add(new UserUsageSummary(entry.getKey(), entry.getValue().totalTokens(), entry.getValue().events())));

        return new AnalyticsReportResponse(
            provider,
            windowStart,
            windowEnd,
            totalEvents,
            allowedEvents,
            deniedEvents,
            totalTokens,
            averageTokens,
            users.size(),
            sampleLimit,
            topUsers
        );
    }

    private long sumTokens(String provider, Instant start, Instant end, int limit) {
        List<TokenUsageByProviderRecord> records =
            byProviderRepository.findByProviderAndTimestampRange(provider, start, end, limit);
        long sum = 0L;
        for (TokenUsageByProviderRecord record : records) {
            sum += record.getTokens();
        }
        return sum;
    }

    private int clamp(Integer value, int defaultValue, int max) {
        if (value == null || value <= 0) {
            return defaultValue;
        }
        return Math.min(value, max);
    }

    private static class UserAggregate {
        private long tokens;
        private int events;

        void add(long tokens) {
            this.tokens += tokens;
            this.events += 1;
        }

        long totalTokens() {
            return tokens;
        }

        int events() {
            return events;
        }
    }
}
