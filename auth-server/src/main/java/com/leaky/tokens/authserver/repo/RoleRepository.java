package com.leaky.tokens.authserver.repo;

import java.util.Optional;
import java.util.UUID;

import com.leaky.tokens.authserver.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);
}
