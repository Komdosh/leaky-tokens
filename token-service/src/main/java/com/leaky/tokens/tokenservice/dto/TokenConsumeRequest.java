package com.leaky.tokens.tokenservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenConsumeRequest {
    private String userId;
    private String orgId;
    private String provider;
    private long tokens;
    private String prompt;
}
