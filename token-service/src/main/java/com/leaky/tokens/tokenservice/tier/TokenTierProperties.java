package com.leaky.tokens.tokenservice.tier;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "token.tiers")
public class TokenTierProperties {
    private String defaultTier = "USER";
    private Map<String, TierConfig> levels = new HashMap<>();

    @Setter
    @Getter
    public static class TierConfig {
        private int priority = 0;
        private double bucketCapacityMultiplier = 1.0;
        private double bucketLeakRateMultiplier = 1.0;
        private Long quotaMaxTokens;
    }
}
