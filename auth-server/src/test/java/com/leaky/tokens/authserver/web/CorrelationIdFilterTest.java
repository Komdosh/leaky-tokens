package com.leaky.tokens.authserver.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    @Test
    void usesIncomingHeader() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (_, _) -> mdcValue.set(MDC.get("correlationId"));

        filter.doFilter(request, response, chain);

        assertThat(mdcValue.get()).isEqualTo("corr-1");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("corr-1");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void generatesHeaderWhenMissing() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValue = new AtomicReference<>();
        FilterChain chain = (_, _) -> mdcValue.set(MDC.get("correlationId"));

        filter.doFilter(request, response, chain);

        String header = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(header).isNotBlank();
        assertThat(mdcValue.get()).isEqualTo(header);
        assertThat(MDC.get("correlationId")).isNull();
    }
}
