package com.leaky.tokens.tokenservice.provider;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class ProviderResponse {
    private String provider;
    private Map<String, Object> data;

    public ProviderResponse() {
    }

    public ProviderResponse(String provider, Map<String, Object> data) {
        this.provider = provider;
        this.data = data;
    }

}
