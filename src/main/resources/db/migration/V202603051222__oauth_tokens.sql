 CREATE TABLE IF NOT EXISTS oauth_tokens (
     id           BIGSERIAL PRIMARY KEY,
     token_type   VARCHAR(20)  NOT NULL,
     token_value  VARCHAR(255) NOT NULL UNIQUE,
     user_id      VARCHAR(100) NOT NULL,
     client_id    VARCHAR(255) NOT NULL,
     redirect_uri VARCHAR(500),
     expires_at   TIMESTAMP    NOT NULL,
     used         BOOLEAN      NOT NULL DEFAULT FALSE,
     created_at   TIMESTAMP    NOT NULL
 );


 CREATE INDEX IF NOT EXISTS idx_token_value_type ON oauth_tokens (token_value, token_type);

 CREATE INDEX IF NOT EXISTS idx_user_id ON oauth_tokens (user_id);