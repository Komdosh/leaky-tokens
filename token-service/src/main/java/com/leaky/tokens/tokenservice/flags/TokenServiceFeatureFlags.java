package com.leaky.tokens.tokenservice.flags;

import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
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

    public boolean isSagaPurchases() {
        return Boolean.TRUE.equals(sagaPurchases);
    }

}
