package com.leaky.tokens.analyticsservice.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table("token_usage_by_provider")
public class TokenUsageByProviderRecord {
    @PrimaryKey
    private TokenUsageByProviderKey key;

    @Column("user_id")
    private String userId;

    @Column("tokens")
    private long tokens;

    @Column("allowed")
    private boolean allowed;

}
