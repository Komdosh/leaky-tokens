package com.leaky.tokens.tokenservice.provider;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.ParameterizedTypeReference;

@Component
@RequiredArgsConstructor
public class WebClientProviderClient implements ProviderClient {
    private final WebClient.Builder webClientBuilder;
    private final ProviderRegistry registry;

    @Override
    public ProviderResponse call(String provider, ProviderRequest request) {
        String baseUrl = registry.baseUrl(provider);
        String path = "/api/v1/" + provider.toLowerCase() + "/chat";

        ParameterizedTypeReference<Map<String, Object>> responseType =
            new ParameterizedTypeReference<>() {};

        Map<String, Object> response = webClientBuilder.build()
            .post()
            .uri(baseUrl + path)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(responseType)
            .block();

        return new ProviderResponse(provider, response == null ? Map.of() : response);
    }
}
