-- V202603051222__oauth_tokens.sql
-- Idempotent + safe for repeated execution

CREATE TABLE IF NOT EXISTS oauth_tokens (
    id           BIGSERIAL PRIMARY KEY,
    token_type   VARCHAR(20)  NOT NULL,
    token_value  VARCHAR(255) NOT NULL,
    user_id      VARCHAR(100) NOT NULL,
    client_id    VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(500),
    expires_at   TIMESTAMP    NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL
);

-- Unique constraint handled safely (important)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_oauth_token_value'
    ) THEN
        ALTER TABLE oauth_tokens
        ADD CONSTRAINT uk_oauth_token_value UNIQUE (token_value);
    END IF;
END $$;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_oauth_token_value_type
ON oauth_tokens (token_value, token_type);

CREATE INDEX IF NOT EXISTS idx_oauth_user_id
ON oauth_tokens (user_id);