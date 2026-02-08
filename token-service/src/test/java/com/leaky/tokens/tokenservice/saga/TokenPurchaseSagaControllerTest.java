package com.leaky.tokens.tokenservice.saga;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.leaky.tokens.tokenservice.tier.TokenTierProperties;
import com.leaky.tokens.tokenservice.tier.TokenTierResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
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
    void rejectsBlankProvider() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\" \",\"tokens\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("provider is required"));
    }

    @Test
    void rejectsNonPositiveTokens() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("tokens must be positive"));
    }

    @Test
    void rejectsTrimmedLongIdempotencyKey() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        String key = "   " + "k".repeat(101) + " ";
        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("idempotency key too long"));
    }

    @Test
    void returnsConflictOnIdempotencyCollision() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        when(tierResolver.resolveTier()).thenReturn(new TokenTierProperties.TierConfig());
        when(sagaService.start(any(TokenPurchaseRequest.class), any(TokenTierProperties.TierConfig.class), any()))
            .thenThrow(new IdempotencyConflictException("purchase already processed"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("purchase already processed"));
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

    @Test
    void rejectsInvalidUserId() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        when(tierResolver.resolveTier()).thenReturn(new TokenTierProperties.TierConfig());
        when(sagaService.start(any(TokenPurchaseRequest.class), any(TokenTierProperties.TierConfig.class), any()))
            .thenThrow(new IllegalArgumentException("bad id"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"not-a-uuid\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("invalid userId or orgId"));
    }

    @Test
    void passesIdempotencyKeyToService() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        TokenTierProperties.TierConfig tier = new TokenTierProperties.TierConfig();
        when(tierResolver.resolveTier()).thenReturn(tier);
        when(sagaService.start(any(TokenPurchaseRequest.class), any(TokenTierProperties.TierConfig.class), any()))
            .thenReturn(new TokenPurchaseResponse(UUID.randomUUID(), TokenPurchaseSagaStatus.STARTED, Instant.now()));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "idem-123")
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isAccepted());

        ArgumentCaptor<TokenPurchaseRequest> requestCaptor = ArgumentCaptor.forClass(TokenPurchaseRequest.class);
        verify(sagaService).start(requestCaptor.capture(), eq(tier), eq("idem-123"));
        assertThat(requestCaptor.getValue().getProvider()).isEqualTo("openai");
    }

    @Test
    void allowsIdempotencyKeyAtLimit() throws Exception {
        TokenPurchaseSagaService sagaService = Mockito.mock(TokenPurchaseSagaService.class);
        TokenTierResolver tierResolver = Mockito.mock(TokenTierResolver.class);
        when(tierResolver.resolveTier()).thenReturn(new TokenTierProperties.TierConfig());
        when(sagaService.start(any(TokenPurchaseRequest.class), any(TokenTierProperties.TierConfig.class), any()))
            .thenReturn(new TokenPurchaseResponse(UUID.randomUUID(), TokenPurchaseSagaStatus.STARTED, Instant.now()));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaController(sagaService, tierResolver)).build();

        String key = "k".repeat(100);
        mockMvc.perform(post("/api/v1/tokens/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", key)
                .content("{\"userId\":\"00000000-0000-0000-0000-000000000001\",\"provider\":\"openai\",\"tokens\":10}"))
            .andExpect(status().isAccepted());
    }
}
