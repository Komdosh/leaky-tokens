package com.leaky.tokens.authserver.dto;

import java.time.Instant;

public class ErrorResponse {
    private final String message;
    private final Instant timestamp;

    public ErrorResponse(String message, Instant timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
