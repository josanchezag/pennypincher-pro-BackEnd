-- V4__create_income_sources.sql
CREATE TABLE income_sources (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(150)  NOT NULL,
    category    VARCHAR(30)   NOT NULL,
    amount      NUMERIC(18,2) NOT NULL,
    currency    VARCHAR(3)    NOT NULL DEFAULT 'COP',
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_income_sources_name     CHECK (btrim(name) <> ''),
    CONSTRAINT ck_income_sources_amount   CHECK (amount >= 0),
    CONSTRAINT ck_income_sources_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_income_sources_category CHECK (category IN
        ('EMPLOYMENT', 'RENTAL', 'BUSINESS', 'INVESTMENT', 'FREELANCE', 'OTHER'))
);

CREATE TRIGGER trg_income_sources_updated_at
    BEFORE UPDATE ON income_sources
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  income_sources          IS 'Recurring monthly income streams';
COMMENT ON COLUMN income_sources.amount   IS 'Monthly amount in COP';
COMMENT ON COLUMN income_sources.currency IS 'ISO 4217 code; reference only, amounts are COP';
