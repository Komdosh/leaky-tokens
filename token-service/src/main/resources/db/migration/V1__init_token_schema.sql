CREATE TABLE IF NOT EXISTS token_pools (
    id UUID PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    user_id UUID,
    total_tokens BIGINT NOT NULL,
    remaining_tokens BIGINT NOT NULL,
    reset_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS token_usage (
    id UUID PRIMARY KEY,
    user_id UUID,
    provider VARCHAR(50) NOT NULL,
    tokens_consumed INT NOT NULL,
    request_timestamp TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_token_pool_user_provider ON token_pools(user_id, provider);
CREATE INDEX IF NOT EXISTS idx_token_usage_user_timestamp ON token_usage(user_id, request_timestamp);
