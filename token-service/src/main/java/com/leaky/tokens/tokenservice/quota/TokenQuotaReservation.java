package com.leaky.tokens.tokenservice.quota;

public record TokenQuotaReservation(boolean allowed, long total, long remaining) {
}
