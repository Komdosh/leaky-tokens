package com.leaky.tokens.analyticsservice.events;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
public class TokenUsageEvent {
    private String userId;
    private String provider;
    private long tokens;
    private boolean allowed;
    private Instant timestamp;
}
