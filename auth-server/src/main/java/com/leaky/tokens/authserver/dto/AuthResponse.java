package com.leaky.tokens.authserver.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(UUID userId, String username, String token, List<String> roles) {
}
