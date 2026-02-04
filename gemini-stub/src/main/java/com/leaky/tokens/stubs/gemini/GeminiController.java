package com.leaky.tokens.stubs.gemini;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeminiController {
    @PostMapping("/api/v1/gemini/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", UUID.randomUUID().toString());
        response.put("provider", "gemini");
        response.put("received", request);
        response.put("created", Instant.now().toString());
        response.put("message", "Stubbed Gemini response");
        return response;
    }
}
