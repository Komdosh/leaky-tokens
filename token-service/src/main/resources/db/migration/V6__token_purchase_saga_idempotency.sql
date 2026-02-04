ALTER TABLE token_purchase_saga
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS idx_token_purchase_saga_idempotency
    ON token_purchase_saga(idempotency_key)
    WHERE idempotency_key IS NOT NULL;
