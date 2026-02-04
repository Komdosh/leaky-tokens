package com.leaky.tokens.tokenservice.tier;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "token.tiers")
public class TokenTierProperties {
    @NotBlank
    private String defaultTier = "USER";
    @Valid
    private Map<String, TierConfig> levels = new HashMap<>();

    @Setter
    @Getter
    public static class TierConfig {
        @Min(0)
        private int priority = 0;
        @Positive
        private double bucketCapacityMultiplier = 1.0;
        @Positive
        private double bucketLeakRateMultiplier = 1.0;
        @Min(0)
        private Long quotaMaxTokens;
    }
}
