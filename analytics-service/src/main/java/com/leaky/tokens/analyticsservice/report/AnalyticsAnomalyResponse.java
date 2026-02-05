package com.leaky.tokens.analyticsservice.report;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
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

}
