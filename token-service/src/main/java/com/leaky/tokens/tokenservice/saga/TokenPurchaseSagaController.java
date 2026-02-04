package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;

import com.leaky.tokens.tokenservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenPurchaseSagaController {
    private final TokenPurchaseSagaService sagaService;

    public TokenPurchaseSagaController(TokenPurchaseSagaService sagaService) {
        this.sagaService = sagaService;
    }

    @PostMapping("/api/v1/tokens/purchase")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> purchase(@RequestBody TokenPurchaseRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("userId is required", Instant.now()));
        }
        if (request.getProvider() == null || request.getProvider().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("provider is required", Instant.now()));
        }
        if (request.getTokens() <= 0) {
            return ResponseEntity.badRequest().body(new ErrorResponse("tokens must be positive", Instant.now()));
        }

        try {
            TokenPurchaseResponse response = sagaService.start(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid userId", Instant.now()));
        }
    }
}
