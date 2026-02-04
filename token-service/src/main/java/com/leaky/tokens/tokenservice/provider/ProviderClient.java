package com.leaky.tokens.tokenservice.provider;

public interface ProviderClient {
    ProviderResponse call(String provider, ProviderRequest request);
}
