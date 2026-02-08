package com.leaky.tokens.stubs.openai;

import com.leaky.tokens.stubs.openai.web.CorrelationIdFilter;
import com.leaky.tokens.stubs.openai.web.SecurityHeadersFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiControllerTest {
    @Test
    void chatReturnsStubbedResponseAndHeaders() throws Exception {
        OpenAiController controller = new OpenAiController();
        Map<String, Object> response = controller.chat(Map.of("prompt", "hi"));

        assertThat(response)
            .containsEntry("provider", "openai")
            .containsEntry("message", "Stubbed OpenAI response");
        assertThat(response.get("received")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> received = (Map<String, Object>) response.get("received");
        assertThat(received).containsEntry("prompt", "hi");

        SecurityHeadersFilter headersFilter = new SecurityHeadersFilter();
        CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/openai/chat");
        MockHttpServletResponse responseObject = new MockHttpServletResponse();

        headersFilter.doFilter(request, responseObject, (req, res) -> {
        });
        assertThat(responseObject.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(responseObject.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(responseObject.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(responseObject.getHeader("Permissions-Policy"))
            .isEqualTo("geolocation=(), microphone=(), camera=()");
        assertThat(responseObject.getHeader("Strict-Transport-Security"))
            .isEqualTo("max-age=31536000; includeSubDomains");

        MockHttpServletResponse correlationResponse = new MockHttpServletResponse();
        correlationIdFilter.doFilter(request, correlationResponse, (req, res) -> {
        });
        assertThat(correlationResponse.getHeader(CorrelationIdFilter.HEADER_NAME)).isNotBlank();
    }

    @Test
    void usesProvidedCorrelationId() throws Exception {
        CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/openai/chat");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        correlationIdFilter.doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("corr-123");
    }
}
