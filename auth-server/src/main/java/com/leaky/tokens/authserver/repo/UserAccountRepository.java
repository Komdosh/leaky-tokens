package com.leaky.tokens.authserver.repo;

import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);
}
