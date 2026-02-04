package com.leaky.tokens.analyticsservice.report;

import java.time.Instant;
import java.util.List;

public class AnalyticsReportResponse {
    private String provider;
    private Instant windowStart;
    private Instant windowEnd;
    private int totalEvents;
    private int allowedEvents;
    private int deniedEvents;
    private long totalTokens;
    private double averageTokensPerEvent;
    private int uniqueUsers;
    private int sampleLimit;
    private List<UserUsageSummary> topUsers;

    public AnalyticsReportResponse(String provider,
                                   Instant windowStart,
                                   Instant windowEnd,
                                   int totalEvents,
                                   int allowedEvents,
                                   int deniedEvents,
                                   long totalTokens,
                                   double averageTokensPerEvent,
                                   int uniqueUsers,
                                   int sampleLimit,
                                   List<UserUsageSummary> topUsers) {
        this.provider = provider;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.totalEvents = totalEvents;
        this.allowedEvents = allowedEvents;
        this.deniedEvents = deniedEvents;
        this.totalTokens = totalTokens;
        this.averageTokensPerEvent = averageTokensPerEvent;
        this.uniqueUsers = uniqueUsers;
        this.sampleLimit = sampleLimit;
        this.topUsers = topUsers;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public Instant getWindowEnd() {
        return windowEnd;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getAllowedEvents() {
        return allowedEvents;
    }

    public int getDeniedEvents() {
        return deniedEvents;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public double getAverageTokensPerEvent() {
        return averageTokensPerEvent;
    }

    public int getUniqueUsers() {
        return uniqueUsers;
    }

    public int getSampleLimit() {
        return sampleLimit;
    }

    public List<UserUsageSummary> getTopUsers() {
        return topUsers;
    }
}
