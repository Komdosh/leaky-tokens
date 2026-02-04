package com.leaky.tokens.tokenservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenConsumeRequest {
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private String userId;
    @Schema(example = "10000000-0000-0000-0000-000000000001")
    private String orgId;
    @Schema(example = "openai")
    private String provider;
    @Schema(example = "25")
    private long tokens;
    @Schema(example = "Hello, world")
    private String prompt;
}
