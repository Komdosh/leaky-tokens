package com.leaky.tokens.apigateway.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Gateway")
public class CircuitBreakerFallbackController {
    @RequestMapping("/fallback/{service}")
    @Operation(
        summary = "Circuit breaker fallback",
        responses = @ApiResponse(responseCode = "503", description = "Service unavailable")
    )
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String service) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", service);
        response.put("status", "unavailable");
        response.put("message", "Circuit breaker open");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
