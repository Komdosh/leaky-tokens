package com.leaky.tokens.tokenservice.saga;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenPurchaseRequest {
    private String userId;
    private String provider;
    private long tokens;

}
