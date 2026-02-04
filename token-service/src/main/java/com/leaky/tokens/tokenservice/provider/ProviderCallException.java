package com.leaky.tokens.tokenservice.provider;

public class ProviderCallException extends RuntimeException {
    public ProviderCallException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderCallException(String message) {
        super(message);
    }
}
