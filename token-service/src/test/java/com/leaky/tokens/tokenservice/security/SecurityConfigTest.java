package com.leaky.tokens.tokenservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();
    }

    @Test
    void permits_health_status_and_docs() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tokens/status")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs/test")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void requires_authentication_for_other_endpoints() throws Exception {
        mockMvc.perform(get("/api/v1/secure")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/secure").with(jwt())).andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        TestController testController() {
            return new TestController();
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "user")
                .claim("roles", java.util.List.of("USER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/actuator/health")
        ResponseEntity<String> health() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/api/v1/tokens/status")
        ResponseEntity<String> status() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/v3/api-docs/test")
        ResponseEntity<String> docs() {
            return ResponseEntity.ok("{}");
        }

        @GetMapping("/swagger-ui/index.html")
        ResponseEntity<String> swagger() {
            return ResponseEntity.ok("ok");
        }

        @GetMapping("/api/v1/secure")
        ResponseEntity<String> secure() {
            return ResponseEntity.ok("ok");
        }
    }

}
