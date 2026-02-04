package com.leaky.tokens.analyticsservice.report;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "analytics.report")
public class AnalyticsReportProperties {
    @Min(1)
    private int defaultWindowMinutes = 60;
    @Min(1)
    private int maxWindowMinutes = 1440;
    @Min(1)
    private int maxLimit = 5000;
    @Min(1)
    private int maxBaselineWindows = 24;
    @Min(1)
    private int maxTopUsers = 5;
    @Min(1)
    private int defaultBaselineWindows = 4;
    @DecimalMin("1.0")
    private double defaultAnomalyThresholdMultiplier = 2.0;

    public int getDefaultWindowMinutes() {
        return defaultWindowMinutes;
    }

    public void setDefaultWindowMinutes(int defaultWindowMinutes) {
        this.defaultWindowMinutes = defaultWindowMinutes;
    }

    public int getMaxWindowMinutes() {
        return maxWindowMinutes;
    }

    public void setMaxWindowMinutes(int maxWindowMinutes) {
        this.maxWindowMinutes = maxWindowMinutes;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }

    public int getMaxBaselineWindows() {
        return maxBaselineWindows;
    }

    public void setMaxBaselineWindows(int maxBaselineWindows) {
        this.maxBaselineWindows = maxBaselineWindows;
    }

    public int getMaxTopUsers() {
        return maxTopUsers;
    }

    public void setMaxTopUsers(int maxTopUsers) {
        this.maxTopUsers = maxTopUsers;
    }

    public int getDefaultBaselineWindows() {
        return defaultBaselineWindows;
    }

    public void setDefaultBaselineWindows(int defaultBaselineWindows) {
        this.defaultBaselineWindows = defaultBaselineWindows;
    }

    public double getDefaultAnomalyThresholdMultiplier() {
        return defaultAnomalyThresholdMultiplier;
    }

    public void setDefaultAnomalyThresholdMultiplier(double defaultAnomalyThresholdMultiplier) {
        this.defaultAnomalyThresholdMultiplier = defaultAnomalyThresholdMultiplier;
    }
}
