-- V6__create_credit_cards_and_loans.sql
CREATE TABLE credit_cards (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(150)  NOT NULL,
    monthly_payment  NUMERIC(18,2) NOT NULL,
    total_balance    NUMERIC(18,2) NOT NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_credit_cards_name    CHECK (btrim(name) <> ''),
    CONSTRAINT ck_credit_cards_amounts CHECK (monthly_payment >= 0 AND total_balance >= 0)
);

CREATE TRIGGER trg_credit_cards_updated_at
    BEFORE UPDATE ON credit_cards
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE credit_cards IS 'Credit cards with the chosen monthly payment and outstanding balance';

CREATE TABLE loans (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(150)  NOT NULL,
    monthly_payment  NUMERIC(18,2) NOT NULL,
    total_balance    NUMERIC(18,2) NOT NULL,
    original_amount  NUMERIC(18,2),
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_loans_name            CHECK (btrim(name) <> ''),
    CONSTRAINT ck_loans_amounts         CHECK (monthly_payment >= 0 AND total_balance >= 0),
    CONSTRAINT ck_loans_original_amount CHECK (original_amount IS NULL OR original_amount >= 0)
);

CREATE TRIGGER trg_loans_updated_at
    BEFORE UPDATE ON loans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  loans                 IS 'Bank and personal loans with fixed installment and remaining principal';
COMMENT ON COLUMN loans.original_amount IS 'Optional original principal, used to show repayment progress';
