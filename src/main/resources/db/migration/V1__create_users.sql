-- V1__create_users.sql
-- Users authenticated through Google, their rotating refresh tokens, and the shared
-- trigger function that keeps updated_at current on every table.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email        VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    picture_url  TEXT,
    google_sub   VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email      UNIQUE (email),
    CONSTRAINT uq_users_google_sub UNIQUE (google_sub)
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  users            IS 'Authenticated users via Google OAuth2';
COMMENT ON COLUMN users.google_sub IS 'Google subject identifier from ID token';

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_tokens_expiry     CHECK (expires_at > created_at),
    CONSTRAINT ck_refresh_tokens_revocation CHECK (revoked OR revoked_at IS NULL)
);

COMMENT ON TABLE  refresh_tokens            IS 'Opaque refresh tokens; rotated on every use';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hex digest of the token sent in the ppp_refresh cookie';
