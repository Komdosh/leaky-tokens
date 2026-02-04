package com.leaky.tokens.apigateway.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
    private boolean permitAll = false;

}
