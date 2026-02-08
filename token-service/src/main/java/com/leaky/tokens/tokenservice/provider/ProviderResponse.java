package com.leaky.tokens.tokenservice.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
public class ProviderResponse {
    private String provider;
    private Map<String, Object> data;
}
