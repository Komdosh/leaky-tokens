package com.leaky.tokens.tokenservice.saga;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TokenPurchaseSagaQueryControllerTest {

    @Test
    void returnsSagaWhenFound() throws Exception {
        TokenPurchaseSagaRepository repository = Mockito.mock(TokenPurchaseSagaRepository.class);
        UUID sagaId = UUID.randomUUID();
        TokenPurchaseSaga saga = new TokenPurchaseSaga(
            sagaId,
            UUID.randomUUID(),
            null,
            "openai",
            100,
            TokenPurchaseSagaStatus.STARTED
        );
        saga.setCreatedAt(Instant.now());

        when(repository.findById(sagaId)).thenReturn(Optional.of(saga));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaQueryController(repository)).build();

        mockMvc.perform(get("/api/v1/tokens/purchase/{sagaId}", sagaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(sagaId.toString()))
            .andExpect(jsonPath("$.provider").value("openai"))
            .andExpect(jsonPath("$.tokens").value(100))
            .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    void returnsNotFoundWhenMissing() throws Exception {
        TokenPurchaseSagaRepository repository = Mockito.mock(TokenPurchaseSagaRepository.class);
        UUID sagaId = UUID.randomUUID();
        when(repository.findById(sagaId)).thenReturn(Optional.empty());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaQueryController(repository)).build();

        mockMvc.perform(get("/api/v1/tokens/purchase/{sagaId}", sagaId))
            .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestWhenIdInvalid() throws Exception {
        TokenPurchaseSagaRepository repository = Mockito.mock(TokenPurchaseSagaRepository.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenPurchaseSagaQueryController(repository)).build();

        mockMvc.perform(get("/api/v1/tokens/purchase/{sagaId}", "not-a-uuid"))
            .andExpect(status().isBadRequest());
    }
}
