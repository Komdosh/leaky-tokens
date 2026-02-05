package com.leaky.tokens.analyticsservice.report;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AnalyticsReportResponse {
    @Schema(example = "openai")
    private String provider;
    @Schema(example = "2026-02-04T16:00:00Z")
    private Instant windowStart;
    @Schema(example = "2026-02-04T17:00:00Z")
    private Instant windowEnd;
    @Schema(example = "120")
    private int totalEvents;
    @Schema(example = "110")
    private int allowedEvents;
    @Schema(example = "10")
    private int deniedEvents;
    @Schema(example = "25000")
    private long totalTokens;
    @Schema(example = "208.33")
    private double averageTokensPerEvent;
    @Schema(example = "5")
    private int uniqueUsers;
    @Schema(example = "500")
    private int sampleLimit;
    @Schema(description = "Top users by token usage in the reporting window")
    private List<UserUsageSummary> topUsers;

}
