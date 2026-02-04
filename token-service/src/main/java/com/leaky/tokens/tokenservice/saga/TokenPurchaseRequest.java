package com.leaky.tokens.tokenservice.saga;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenPurchaseRequest {
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private String userId;
    @Schema(example = "10000000-0000-0000-0000-000000000001")
    private String orgId;
    @Schema(example = "openai")
    private String provider;
    @Schema(example = "1000")
    private long tokens;

}
