package com.leaky.tokens.apigateway.ratelimit;

public enum RateLimitKeyStrategy {
    IP,
    API_KEY_HEADER,
    USER_HEADER
}
