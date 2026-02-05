package com.leaky.tokens.analyticsservice.report;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
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

}
