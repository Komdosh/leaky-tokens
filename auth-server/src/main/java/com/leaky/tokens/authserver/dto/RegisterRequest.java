package com.leaky.tokens.authserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    @Schema(example = "demo-user")
    private String username;
    @Schema(example = "demo-user@example.com")
    private String email;
    @Schema(example = "password")
    private String password;

}
