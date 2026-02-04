package com.leaky.tokens.tokenservice.flags;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "feature.flags.token-service")
public class TokenServiceFeatureFlags {
    @NotNull
    private Boolean quotaEnforcement = true;
    @NotNull
    private Boolean sagaPurchases = true;

    public boolean isQuotaEnforcement() {
        return Boolean.TRUE.equals(quotaEnforcement);
    }

    public void setQuotaEnforcement(Boolean quotaEnforcement) {
        this.quotaEnforcement = quotaEnforcement;
    }

    public boolean isSagaPurchases() {
        return Boolean.TRUE.equals(sagaPurchases);
    }

    public void setSagaPurchases(Boolean sagaPurchases) {
        this.sagaPurchases = sagaPurchases;
    }
}
