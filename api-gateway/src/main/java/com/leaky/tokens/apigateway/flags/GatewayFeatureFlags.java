package com.leaky.tokens.apigateway.flags;

import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
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

    public boolean isApiKeyValidation() {
        return Boolean.TRUE.equals(apiKeyValidation);
    }

}
