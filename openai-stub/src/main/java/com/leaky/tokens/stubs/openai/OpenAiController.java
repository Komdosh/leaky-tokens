package com.leaky.tokens.stubs.openai;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenAiController {
    @PostMapping("/api/v1/openai/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", UUID.randomUUID().toString());
        response.put("provider", "openai");
        response.put("received", request);
        response.put("created", Instant.now().toString());
        response.put("message", "Stubbed OpenAI response");
        return response;
    }
}
