CREATE TABLE IF NOT EXISTS token_org_pools (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    total_tokens BIGINT NOT NULL,
    remaining_tokens BIGINT NOT NULL,
    reset_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_token_org_pool_org_provider ON token_org_pools(org_id, provider);
