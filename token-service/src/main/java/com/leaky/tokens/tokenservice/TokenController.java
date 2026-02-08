package com.leaky.tokens.tokenservice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.leaky.tokens.tokenservice.bucket.TokenBucketResult;
import com.leaky.tokens.tokenservice.bucket.TokenBucketService;
import com.leaky.tokens.tokenservice.dto.ErrorResponse;
import com.leaky.tokens.tokenservice.dto.TokenConsumeRequest;
import com.leaky.tokens.tokenservice.dto.TokenConsumeResponse;
import com.leaky.tokens.tokenservice.metrics.TokenServiceMetrics;
import com.leaky.tokens.tokenservice.provider.ProviderCallException;
import com.leaky.tokens.tokenservice.provider.ProviderCallService;
import com.leaky.tokens.tokenservice.provider.ProviderRequest;
import com.leaky.tokens.tokenservice.provider.ProviderResponse;
import com.leaky.tokens.tokenservice.quota.TokenQuotaReservation;
import com.leaky.tokens.tokenservice.quota.TokenQuotaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@Tag(name = "Tokens")
@RequiredArgsConstructor
public class TokenController {
    private final TokenBucketService tokenBucketService;
    private final ProviderCallService providerCallService;
    private final TokenQuotaService quotaService;
    private final TokenServiceMetrics metrics;
    private final TokenTierResolver tierResolver;

    @GetMapping("/api/v1/tokens/status")
    @Operation(
        summary = "Service status",
        responses = @ApiResponse(responseCode = "200", description = "Service is up")
    )
    public Map<String, Object> status() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "token-service");
        response.put("status", "ok");
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    @GetMapping("/api/v1/tokens/quota")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Fetch user quota",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Quota found"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quota not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<?> quota(@RequestParam("userId") String userId,
                                   @RequestParam("provider") String provider) {
        if (userId == null || userId.isBlank()) {
            metrics.quotaLookup("unknown", "invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse("userId is required", Instant.now()));
        }
        if (provider == null || provider.isBlank()) {
            metrics.quotaLookup("unknown", "invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse("provider is required", Instant.now()));
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId.trim());
        } catch (IllegalArgumentException ex) {
            metrics.quotaLookup(provider.trim(), "invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid userId", Instant.now()));
        }

        TokenTierProperties.TierConfig tier = tierResolver.resolveTier();
        return quotaService.getQuota(userUuid, provider.trim(), tier)
            .<ResponseEntity<?>>map(pool -> {
                metrics.quotaLookup(provider.trim(), "found");
                return ResponseEntity.ok(Map.of(
                    "userId", pool.getUserId(),
                    "provider", pool.getProvider(),
                    "totalTokens", pool.getTotalTokens(),
                    "remainingTokens", pool.getRemainingTokens(),
                    "updatedAt", pool.getUpdatedAt()
                ));
            })
            .orElseGet(() -> {
                metrics.quotaLookup(provider.trim(), "not_found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("quota not found", Instant.now()));
            });
    }

    @GetMapping("/api/v1/tokens/quota/org")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Fetch org quota",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Org quota found"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quota not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<?> orgQuota(@RequestParam("orgId") String orgId,
                                      @RequestParam("provider") String provider) {
        if (orgId == null || orgId.isBlank()) {
            metrics.quotaLookup("unknown", "invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse("orgId is required", Instant.now()));
        }
        if (provider == null || provider.isBlank()) {
            metrics.quotaLookup("unknown", "invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse("provider is required", Instant.now()));
        }

        UUID orgUuid;
        try {
            orgUuid = UUID.fromString(orgId.trim());
        } catch (IllegalArgumentException ex) {
            metrics.quotaLookup(provider.trim(), "invalid");
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid orgId", Instant.now()));
        }

        TokenTierProperties.TierConfig tier = tierResolver.resolveTier();
        return quotaService.getOrgQuota(orgUuid, provider.trim(), tier)
            .<ResponseEntity<?>>map(pool -> {
                metrics.quotaLookup(provider.trim(), "found");
                return ResponseEntity.ok(Map.of(
                    "orgId", pool.getOrgId(),
                    "provider", pool.getProvider(),
                    "totalTokens", pool.getTotalTokens(),
                    "remainingTokens", pool.getRemainingTokens(),
                    "updatedAt", pool.getUpdatedAt()
                ));
            })
            .orElseGet(() -> {
                metrics.quotaLookup(provider.trim(), "not_found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("quota not found", Instant.now()));
            });
    }

    @PostMapping("/api/v1/tokens/consume")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Consume tokens (user or org quota)",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Tokens consumed"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Insufficient quota", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limited", content = @Content(schema = @Schema(implementation = TokenConsumeResponse.class))),
            @ApiResponse(responseCode = "502", description = "Provider call failed", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<?> consume(@RequestBody TokenConsumeRequest request, HttpServletRequest httpRequest) {
        String userId = request.getUserId();
        String orgId = request.getOrgId();
        String provider = request.getProvider();
        long tokens = request.getTokens();

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("userId is required", Instant.now()));
        }
        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("provider is required", Instant.now()));
        }
        if (tokens <= 0) {
            return ResponseEntity.badRequest().body(new ErrorResponse("tokens must be positive", Instant.now()));
        }

        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId.trim());
        } catch (IllegalArgumentException ex) {
            metrics.consumeQuotaInsufficient(provider == null ? "unknown" : provider.trim());
            return ResponseEntity.badRequest().body(new ErrorResponse("invalid userId", Instant.now()));
        }
        UUID orgUuid = null;
        if (orgId != null && !orgId.isBlank()) {
            try {
                orgUuid = UUID.fromString(orgId.trim());
            } catch (IllegalArgumentException ex) {
                metrics.consumeQuotaInsufficient(provider == null ? "unknown" : provider.trim());
                return ResponseEntity.badRequest().body(new ErrorResponse("invalid orgId", Instant.now()));
            }
        }

        metrics.consumeAttempt(provider.trim());
        TokenTierProperties.TierConfig tier = tierResolver.resolveTier();
        TokenQuotaReservation reservation;
        if (orgUuid == null) {
            reservation = quotaService.reserve(userUuid, provider.trim(), tokens, tier);
        } else {
            reservation = quotaService.reserveOrg(orgUuid, provider.trim(), tokens, tier);
        }
        if (!reservation.allowed()) {
            metrics.consumeQuotaInsufficient(provider.trim());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ErrorResponse("insufficient token quota", Instant.now()));
        }

        TokenBucketResult result = tokenBucketService.consume(userId.trim(), provider.trim(), tokens, tier);
        httpRequest.setAttribute("tokenBucketResult", result);

        if (!result.isAllowed()) {
            if (orgUuid == null) {
                quotaService.release(userUuid, provider.trim(), tokens, tier);
            } else {
                quotaService.releaseOrg(orgUuid, provider.trim(), tokens, tier);
            }
            metrics.consumeRateLimited(provider.trim());
            TokenConsumeResponse response = new TokenConsumeResponse(
                false,
                result.getCapacity(),
                result.getUsed(),
                result.getRemaining(),
                result.getWaitSeconds(),
                result.getTimestamp(),
                Map.of()
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }

        ProviderResponse providerResponse;
        try {
            providerResponse = providerCallService.call(provider.trim(), new ProviderRequest(request.getPrompt()));
        } catch (ProviderCallException ex) {
            if (orgUuid == null) {
                quotaService.release(userUuid, provider.trim(), tokens, tier);
            } else {
                quotaService.releaseOrg(orgUuid, provider.trim(), tokens, tier);
            }
            metrics.consumeProviderFailure(provider.trim());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("provider call failed", Instant.now()));
        }

        metrics.consumeAllowed(provider.trim());
        TokenConsumeResponse response = new TokenConsumeResponse(
            true,
            result.getCapacity(),
            result.getUsed(),
            result.getRemaining(),
            result.getWaitSeconds(),
            result.getTimestamp(),
            providerResponse.getData()
        );

        return ResponseEntity.ok(response);
    }
}
