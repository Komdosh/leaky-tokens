package com.leaky.tokens.stubs.qwen;

import com.leaky.tokens.stubs.qwen.web.CorrelationIdFilter;
import com.leaky.tokens.stubs.qwen.web.SecurityHeadersFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QwenController.class)
@Import({SecurityHeadersFilter.class, CorrelationIdFilter.class})
class QwenControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatReturnsStubbedResponseAndHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/qwen/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"hi\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("qwen"))
            .andExpect(jsonPath("$.message").value("Stubbed Qwen response"))
            .andExpect(jsonPath("$.received.prompt").value("hi"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("Permissions-Policy", "geolocation=(), microphone=(), camera=()"))
            .andExpect(header().string("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
            .andExpect(header().exists(CorrelationIdFilter.HEADER_NAME));
    }

    @Test
    void usesProvidedCorrelationId() throws Exception {
        mockMvc.perform(post("/api/v1/qwen/chat")
                .header(CorrelationIdFilter.HEADER_NAME, "corr-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prompt\":\"hi\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "corr-123"));
    }
}
