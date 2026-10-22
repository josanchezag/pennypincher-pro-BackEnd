-- V2__create_assets_and_liabilities.sql
CREATE TABLE assets (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(150)  NOT NULL,
    value       NUMERIC(18,2) NOT NULL,
    category    VARCHAR(30)   NOT NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assets_name     CHECK (btrim(name) <> ''),
    CONSTRAINT ck_assets_value    CHECK (value >= 0),
    CONSTRAINT ck_assets_category CHECK (category IN
        ('REAL_ESTATE', 'VEHICLE', 'BUSINESS', 'CASH', 'SAVINGS', 'EQUIPMENT', 'OTHER'))
);

CREATE TRIGGER trg_assets_updated_at
    BEFORE UPDATE ON assets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  assets           IS 'Things the user owns, valued in COP';
COMMENT ON COLUMN assets.is_active IS 'Soft delete flag';

CREATE TABLE liabilities (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(150)  NOT NULL,
    current_balance  NUMERIC(18,2) NOT NULL,
    category         VARCHAR(30)   NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_liabilities_name            CHECK (btrim(name) <> ''),
    CONSTRAINT ck_liabilities_current_balance CHECK (current_balance >= 0),
    CONSTRAINT ck_liabilities_category        CHECK (category IN
        ('CREDIT_CARD', 'PERSONAL_LOAN', 'BANK_LOAN', 'FAMILY_DEBT', 'OTHER'))
);

CREATE TRIGGER trg_liabilities_updated_at
    BEFORE UPDATE ON liabilities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  liabilities           IS 'Debts the user owes, balances in COP';
COMMENT ON COLUMN liabilities.is_active IS 'Soft delete flag';
