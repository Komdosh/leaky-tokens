package com.leaky.tokens.analyticsservice.report;

import java.time.Instant;

public class AnalyticsAnomalyResponse {
    private String provider;
    private Instant windowStart;
    private Instant windowEnd;
    private int baselineWindows;
    private long currentTokens;
    private double baselineAverageTokens;
    private double ratio;
    private double thresholdMultiplier;
    private boolean anomaly;
    private int sampleLimit;

    public AnalyticsAnomalyResponse(String provider,
                                    Instant windowStart,
                                    Instant windowEnd,
                                    int baselineWindows,
                                    long currentTokens,
                                    double baselineAverageTokens,
                                    double ratio,
                                    double thresholdMultiplier,
                                    boolean anomaly,
                                    int sampleLimit) {
        this.provider = provider;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.baselineWindows = baselineWindows;
        this.currentTokens = currentTokens;
        this.baselineAverageTokens = baselineAverageTokens;
        this.ratio = ratio;
        this.thresholdMultiplier = thresholdMultiplier;
        this.anomaly = anomaly;
        this.sampleLimit = sampleLimit;
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

    public int getBaselineWindows() {
        return baselineWindows;
    }

    public long getCurrentTokens() {
        return currentTokens;
    }

    public double getBaselineAverageTokens() {
        return baselineAverageTokens;
    }

    public double getRatio() {
        return ratio;
    }

    public double getThresholdMultiplier() {
        return thresholdMultiplier;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public int getSampleLimit() {
        return sampleLimit;
    }
}
