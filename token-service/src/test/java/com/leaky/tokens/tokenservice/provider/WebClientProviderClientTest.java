package com.leaky.tokens.tokenservice.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebClientProviderClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ProviderRegistry registry;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    @InjectMocks
    private WebClientProviderClient client;

    @Test
    void call_builds_expected_request_and_returns_response() {
        ProviderRequest request = new ProviderRequest("hello");
        when(registry.baseUrl("openai")).thenReturn("http://provider-host");
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://provider-host/api/v1/openai/chat")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(request)).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Map.of("answer", "ok")));

        ProviderResponse response = client.call("openai", request);

        assertThat(response.getProvider()).isEqualTo("openai");
        assertThat(response.getData()).containsEntry("answer", "ok");
        verify(registry).baseUrl("openai");
        verify(requestBodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec).bodyValue(request);
    }

    @Test
    void call_returns_empty_map_when_provider_returns_null_body() {
        ProviderRequest request = new ProviderRequest("hello");
        when(registry.baseUrl("qwen")).thenReturn("http://provider-host");
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://provider-host/api/v1/qwen/chat")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(request)).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.empty());

        ProviderResponse response = client.call("qwen", request);

        assertThat(response.getProvider()).isEqualTo("qwen");
        assertThat(response.getData()).isEmpty();
    }

    @Test
    void call_uses_lowercase_provider_in_path() {
        ProviderRequest request = new ProviderRequest("hello");
        when(registry.baseUrl("OpenAI")).thenReturn("http://provider-host");
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(request)).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Map.of()));

        client.call("OpenAI", request);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue()).endsWith("/api/v1/openai/chat");
    }
}
