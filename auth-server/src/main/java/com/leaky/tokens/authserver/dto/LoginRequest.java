package com.leaky.tokens.authserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {
    @Schema(example = "demo-user")
    private String username;
    @Schema(example = "password")
    private String password;

}
