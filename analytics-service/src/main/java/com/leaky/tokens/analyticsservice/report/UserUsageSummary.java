package com.leaky.tokens.analyticsservice.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserUsageSummary {
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private String userId;
    @Schema(example = "12000")
    private long totalTokens;
    @Schema(example = "42")
    private int events;
}
