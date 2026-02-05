package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenPurchaseResponse(@Schema(example = "22222222-2222-2222-2222-222222222222") UUID sagaId,
                                    @Schema(example = "STARTED") TokenPurchaseSagaStatus status,
                                    @Schema(example = "2026-02-04T17:00:00Z") Instant createdAt){

}
