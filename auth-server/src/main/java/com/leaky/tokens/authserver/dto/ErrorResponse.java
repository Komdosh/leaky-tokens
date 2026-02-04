package com.leaky.tokens.authserver.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {
}
