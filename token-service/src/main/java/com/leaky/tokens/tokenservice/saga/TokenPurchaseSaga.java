package com.leaky.tokens.tokenservice.saga;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "token_purchase_saga")
public class TokenPurchaseSaga implements Persistable<UUID> {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "org_id")
    private UUID orgId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false)
    private long tokens;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TokenPurchaseSagaStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TokenPurchaseSaga(UUID id, UUID userId, UUID orgId, String provider, long tokens, TokenPurchaseSagaStatus status) {
        this.id = id;
        this.userId = userId;
        this.orgId = orgId;
        this.provider = provider;
        this.tokens = tokens;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @Override
    @Transient
    public boolean isNew() {
        return createdAt == null;
    }
}
