package com.leaky.tokens.analyticsservice.storage;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Composite key (provider + timestamp)")
    @PrimaryKey
    private TokenUsageByProviderKey key;

    @Column("user_id")
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private String userId;

    @Column("tokens")
    @Schema(example = "25")
    private long tokens;

    @Column("allowed")
    @Schema(example = "true")
    private boolean allowed;

}
