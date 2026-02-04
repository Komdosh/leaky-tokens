package com.leaky.tokens.apigateway.flags;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "feature.flags.gateway")
public class GatewayFeatureFlags {
    @NotNull
    private Boolean rateLimiting = true;
    @NotNull
    private Boolean apiKeyValidation = true;

    public boolean isRateLimiting() {
        return Boolean.TRUE.equals(rateLimiting);
    }

    public void setRateLimiting(Boolean rateLimiting) {
        this.rateLimiting = rateLimiting;
    }

    public boolean isApiKeyValidation() {
        return Boolean.TRUE.equals(apiKeyValidation);
    }

    public void setApiKeyValidation(Boolean apiKeyValidation) {
        this.apiKeyValidation = apiKeyValidation;
    }
}
