package com.leaky.tokens.tokenservice.saga;

import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenPurchaseSagaQueryController {
    private final TokenPurchaseSagaRepository repository;

    public TokenPurchaseSagaQueryController(TokenPurchaseSagaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/tokens/purchase/{sagaId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getSaga(@PathVariable String sagaId) {
        try {
            UUID id = UUID.fromString(sagaId);
            Optional<TokenPurchaseSaga> saga = repository.findById(id);
            return saga.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
