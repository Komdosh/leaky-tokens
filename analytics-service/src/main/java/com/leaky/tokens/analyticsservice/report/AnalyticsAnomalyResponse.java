package com.leaky.tokens.analyticsservice.report;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public class AnalyticsAnomalyResponse {
    @Schema(example = "openai")
    private String provider;
    @Schema(example = "2026-02-04T16:00:00Z")
    private Instant windowStart;
    @Schema(example = "2026-02-04T17:00:00Z")
    private Instant windowEnd;
    @Schema(example = "4")
    private int baselineWindows;
    @Schema(example = "50000")
    private long currentTokens;
    @Schema(example = "12000")
    private double baselineAverageTokens;
    @Schema(example = "4.1")
    private double ratio;
    @Schema(example = "2.0")
    private double thresholdMultiplier;
    @Schema(example = "true")
    private boolean anomaly;
    @Schema(example = "500")
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
