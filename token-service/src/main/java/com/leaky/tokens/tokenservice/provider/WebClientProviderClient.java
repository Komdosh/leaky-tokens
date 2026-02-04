package com.leaky.tokens.tokenservice.provider;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebClientProviderClient implements ProviderClient {
    private final WebClient.Builder webClientBuilder;
    private final ProviderRegistry registry;

    public WebClientProviderClient(WebClient.Builder webClientBuilder, ProviderRegistry registry) {
        this.webClientBuilder = webClientBuilder;
        this.registry = registry;
    }

    @Override
    public ProviderResponse call(String provider, ProviderRequest request) {
        String baseUrl = registry.baseUrl(provider);
        String path = "/api/v1/" + provider.toLowerCase() + "/chat";

        Map<String, Object> response = webClientBuilder.build()
            .post()
            .uri(baseUrl + path)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return new ProviderResponse(provider, response == null ? Map.of() : response);
    }
}
