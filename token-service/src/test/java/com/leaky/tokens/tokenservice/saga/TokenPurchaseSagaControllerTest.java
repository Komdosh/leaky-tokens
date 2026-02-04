package com.leaky.tokens.tokenservice.saga;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import com.leaky.tokens.tokenservice.tier.TokenTierResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TokenPurchaseSagaControllerTest {
    @Test
    void rejectsMissingUserId() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("userId is required"));
    }

    @Test
    void rejectsInvalidOrgId() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"orgId\":\"bad\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invalid userId or orgId"));
    }

    @Test
    void rejectsLongIdempotencyKey() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        String key = "k".repeat(101);
        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("idempotency key too long"));
    }

    @Test
    void returnsAcceptedOnSuccess() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        when(tierResolver.resolveTier()).thenReturn(new TokenTierProperties.TierConfig());
        when(sagaService.start(any(TokenPurchaseRequest.class), any(TokenTierProperties.TierConfig.class), any()))
            .thenReturn(new TokenPurchaseResponse(UUID.randomUUID(), TokenPurchaseSagaStatus.STARTED, Instant.now()));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("STARTED"));
    }
}
