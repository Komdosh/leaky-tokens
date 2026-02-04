package com.leaky.tokens.tokenservice.provider;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProviderRegistry {
    private final Map<String, String> providerBaseUrls = new HashMap<>();

    public ProviderRegistry(
        @Value("${providers.qwen.base-url:http://localhost:8091}") String qwenUrl,
        @Value("${providers.gemini.base-url:http://localhost:8092}") String geminiUrl,
        @Value("${providers.openai.base-url:http://localhost:8093}") String openaiUrl
    ) {
        providerBaseUrls.put("qwen", qwenUrl);
        providerBaseUrls.put("gemini", geminiUrl);
        providerBaseUrls.put("openai", openaiUrl);
    }

    public String baseUrl(String provider) {
        String url = providerBaseUrls.get(provider.toLowerCase());
        if (url == null) {
            throw new IllegalArgumentException("unknown provider: " + provider);
        }
        return url;
    }
}
