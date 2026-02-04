package com.leaky.tokens.tokenservice.web;

import java.io.IOException;

import com.leaky.tokens.tokenservice.bucket.TokenBucketResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RateLimitHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/tokens/consume")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
        } finally {
            Object attr = request.getAttribute("tokenBucketResult");
            if (attr instanceof TokenBucketResult result) {
                wrapper.setHeader("X-RateLimit-Limit", String.valueOf(result.getCapacity()));
                wrapper.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
                wrapper.setHeader("X-RateLimit-Used", String.valueOf(result.getUsed()));
                wrapper.setHeader("X-RateLimit-Reset", String.valueOf(result.getWaitSeconds()));
            }
            wrapper.copyBodyToResponse();
        }
    }
}
