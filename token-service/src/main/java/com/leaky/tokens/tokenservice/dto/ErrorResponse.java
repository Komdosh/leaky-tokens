package com.leaky.tokens.tokenservice.dto;

import java.time.Instant;

public record ErrorResponse(String message, Instant timestamp) {
}
