package com.leaky.tokens.authserver.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserAccountTest {
    @Test
    void onCreateSetsTimestampsAndIdWhenMissing() {
        UserAccount user = new UserAccount(null, "alice", "alice@example.com", "hash");

        user.onCreate();

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void onCreateKeepsExistingId() {
        UUID id = UUID.randomUUID();
        UserAccount user = new UserAccount(id, "alice", "alice@example.com", "hash");

        user.onCreate();

        assertThat(user.getId()).isEqualTo(id);
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        UserAccount user = new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hash");
        user.onCreate();
        Instant initial = user.getUpdatedAt();

        user.onUpdate();

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(initial);
    }

    @Test
    void setRolesHandlesNullAndCopiesSet() {
        UserAccount user = new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hash");
        user.setRoles(null);

        assertThat(user.getRoles()).isEmpty();

        Role role = new Role(UUID.randomUUID(), "USER", "Default user role");
        user.setRoles(Set.of(role));
        assertThat(user.getRoles()).contains(role);
    }

    @Test
    void addRoleIgnoresNull() {
        UserAccount user = new UserAccount(UUID.randomUUID(), "alice", "alice@example.com", "hash");
        user.addRole(null);

        assertThat(user.getRoles()).isEmpty();
    }
}
