package com.leaky.tokens.tokenservice.saga;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenPurchaseSagaRepository extends JpaRepository<TokenPurchaseSaga, UUID> {
}
