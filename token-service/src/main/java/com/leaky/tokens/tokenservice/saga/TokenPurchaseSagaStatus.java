package com.leaky.tokens.tokenservice.saga;

public enum TokenPurchaseSagaStatus {
    STARTED,
    PAYMENT_RESERVED,
    TOKENS_ALLOCATED,
    COMPLETED,
    FAILED
}
