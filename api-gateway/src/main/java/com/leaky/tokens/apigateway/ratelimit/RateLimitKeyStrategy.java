package com.leaky.tokens.apigateway.ratelimit;

public enum RateLimitKeyStrategy {
    IP,
    AUTO,
    API_KEY_HEADER,
    USER_HEADER
}
