-- V7__create_expenses.sql
CREATE TABLE fixed_expenses (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(150)  NOT NULL,
    monthly_amount  NUMERIC(18,2) NOT NULL,
    category        VARCHAR(30)   NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_fixed_expenses_name     CHECK (btrim(name) <> ''),
    CONSTRAINT ck_fixed_expenses_amount   CHECK (monthly_amount >= 0),
    CONSTRAINT ck_fixed_expenses_category CHECK (category IN
        ('FOOD', 'EDUCATION', 'TRANSPORT', 'UTILITIES', 'INSURANCE', 'HOUSING',
         'HEALTH', 'CHARITY', 'COMMUNICATION', 'TAXES', 'OTHER'))
);

CREATE TRIGGER trg_fixed_expenses_updated_at
    BEFORE UPDATE ON fixed_expenses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE fixed_expenses IS 'Recurring, predictable monthly costs';

CREATE TABLE variable_expenses (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(150)  NOT NULL,
    monthly_amount  NUMERIC(18,2) NOT NULL,
    category        VARCHAR(30)   NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_variable_expenses_name     CHECK (btrim(name) <> ''),
    CONSTRAINT ck_variable_expenses_amount   CHECK (monthly_amount >= 0),
    CONSTRAINT ck_variable_expenses_category CHECK (category IN
        ('LEISURE', 'STREAMING', 'HOUSEHOLD_STAFF', 'DINING', 'CLOTHING', 'GIFTS', 'OTHER'))
);

CREATE TRIGGER trg_variable_expenses_updated_at
    BEFORE UPDATE ON variable_expenses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  variable_expenses                IS 'Recurring but variable monthly costs';
COMMENT ON COLUMN variable_expenses.monthly_amount IS 'Budgeted estimate, not an actual transaction';
