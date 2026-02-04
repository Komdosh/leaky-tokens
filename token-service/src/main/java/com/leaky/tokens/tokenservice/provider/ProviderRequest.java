package com.leaky.tokens.tokenservice.provider;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProviderRequest {
    private String prompt;

    public ProviderRequest() {
    }

    public ProviderRequest(String prompt) {
        this.prompt = prompt;
    }

}
