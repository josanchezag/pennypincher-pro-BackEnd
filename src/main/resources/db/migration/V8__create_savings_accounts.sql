-- V8__create_savings_accounts.sql
CREATE TABLE savings_accounts (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                  VARCHAR(150)  NOT NULL,
    monthly_contribution  NUMERIC(18,2) NOT NULL,
    current_balance       NUMERIC(18,2) NOT NULL,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_savings_accounts_name    CHECK (btrim(name) <> ''),
    CONSTRAINT ck_savings_accounts_amounts CHECK (monthly_contribution >= 0 AND current_balance >= 0)
);

CREATE TRIGGER trg_savings_accounts_updated_at
    BEFORE UPDATE ON savings_accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE savings_accounts IS 'Savings or investment vehicles with monthly allocation and balance';
