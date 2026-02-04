package com.leaky.tokens.tokenservice.quota;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "token.quota")
public class TokenQuotaProperties {
    private boolean enabled = true;
    @NotNull
    private Duration window = Duration.ofHours(24);
}
