package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;

import com.leaky.tokens.tokenservice.dto.ErrorResponse;
import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import com.leaky.tokens.tokenservice.tier.TokenTierResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Token Purchase")
@RequiredArgsConstructor
public class TokenPurchaseSagaController {
    private final TokenPurchaseSagaService sagaService;
    private final TokenTierResolver tierResolver;

    @PostMapping("/api/v1/tokens/purchase")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Start token purchase saga",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "202", description = "Saga accepted"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<?> purchase(@RequestBody TokenPurchaseRequest request,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("userId is required", Instant.now()));
        }
        if (request.getProvider() == null || request.getProvider().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("provider is required", Instant.now()));
        }
        if (request.getTokens() <= 0) {
            return ResponseEntity.badRequest().body(new ErrorResponse("tokens must be positive", Instant.now()));
        }
        if (idempotencyKey != null && idempotencyKey.trim().length() > 100) {
            return ResponseEntity.badRequest().body(new ErrorResponse("idempotency key too long", Instant.now()));
        }

        try {
            if (request.getOrgId() != null && !request.getOrgId().isBlank()) {
                java.util.UUID.fromString(request.getOrgId());
            }
            TokenTierProperties.TierConfig tier = tierResolver.resolveTier();
            TokenPurchaseResponse response = sagaService.start(request, tier, idempotencyKey);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IdempotencyConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), Instant.now()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid userId or orgId", Instant.now()));
        }
    }
}
