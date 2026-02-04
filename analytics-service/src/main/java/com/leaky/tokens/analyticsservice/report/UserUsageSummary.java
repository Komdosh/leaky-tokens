package com.leaky.tokens.analyticsservice.report;

public class UserUsageSummary {
    private String userId;
    private long totalTokens;
    private int events;

    public UserUsageSummary(String userId, long totalTokens, int events) {
        this.userId = userId;
        this.totalTokens = totalTokens;
        this.events = events;
    }

    public String getUserId() {
        return userId;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public int getEvents() {
        return events;
    }
}
