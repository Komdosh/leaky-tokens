package com.leaky.tokens.tokenservice.saga;

import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Token Purchase")
public class TokenPurchaseSagaQueryController {
    private final TokenPurchaseSagaRepository repository;

    public TokenPurchaseSagaQueryController(TokenPurchaseSagaRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/v1/tokens/purchase/{sagaId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get saga status",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Saga found"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "400", description = "Invalid saga id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
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
