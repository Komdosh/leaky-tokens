ALTER TABLE token_purchase_saga
    ADD COLUMN IF NOT EXISTS org_id UUID;

CREATE INDEX IF NOT EXISTS idx_token_purchase_saga_org ON token_purchase_saga(org_id);
