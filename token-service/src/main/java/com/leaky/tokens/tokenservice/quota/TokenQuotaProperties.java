package com.leaky.tokens.tokenservice.quota;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "token.quota")
public class TokenQuotaProperties {
    private boolean enabled = true;
    private Duration window = Duration.ofHours(24);
}
