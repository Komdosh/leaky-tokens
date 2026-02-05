package com.leaky.tokens.analyticsservice.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class UserUsageSummary {
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private String userId;
    @Schema(example = "12000")
    private long totalTokens;
    @Schema(example = "42")
    private int events;

    public UserUsageSummary(String userId, long totalTokens, int events) {
        this.userId = userId;
        this.totalTokens = totalTokens;
        this.events = events;
    }

}
