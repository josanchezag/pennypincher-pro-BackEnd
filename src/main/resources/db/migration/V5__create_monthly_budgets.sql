-- V5__create_monthly_budgets.sql
CREATE TABLE monthly_budgets (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    year                    INTEGER       NOT NULL,
    month                   INTEGER       NOT NULL,
    exchange_rate           NUMERIC(18,2),
    work_days               INTEGER,
    total_income            NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_expenses          NUMERIC(18,2) NOT NULL DEFAULT 0,
    monthly_cash_flow       NUMERIC(18,2) NOT NULL DEFAULT 0,
    credit_card_percentage  NUMERIC(9,4)  NOT NULL DEFAULT 0,
    loan_percentage         NUMERIC(9,4)  NOT NULL DEFAULT 0,
    expense_percentage      NUMERIC(9,4)  NOT NULL DEFAULT 0,
    savings_percentage      NUMERIC(9,4)  NOT NULL DEFAULT 0,
    charity_percentage      NUMERIC(9,4)  NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_monthly_budgets_user_period UNIQUE (user_id, year, month),
    CONSTRAINT ck_monthly_budgets_year        CHECK (year BETWEEN 2000 AND 2100),
    CONSTRAINT ck_monthly_budgets_month       CHECK (month BETWEEN 1 AND 12),
    CONSTRAINT ck_monthly_budgets_rate        CHECK (exchange_rate IS NULL OR exchange_rate > 0),
    CONSTRAINT ck_monthly_budgets_work_days   CHECK (work_days IS NULL OR work_days BETWEEN 0 AND 31),
    CONSTRAINT ck_monthly_budgets_totals      CHECK (total_income >= 0 AND total_expenses >= 0),
    CONSTRAINT ck_monthly_budgets_cash_flow   CHECK (monthly_cash_flow = total_income - total_expenses),
    CONSTRAINT ck_monthly_budgets_percentages CHECK (
        credit_card_percentage >= 0 AND loan_percentage >= 0 AND expense_percentage >= 0
        AND savings_percentage >= 0 AND charity_percentage >= 0)
);

CREATE TRIGGER trg_monthly_budgets_updated_at
    BEFORE UPDATE ON monthly_budgets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  monthly_budgets               IS 'Monthly income and expense declaration, one per user per month';
COMMENT ON COLUMN monthly_budgets.exchange_rate IS 'TRM (COP per USD) entered manually as a reference';
COMMENT ON COLUMN monthly_budgets.work_days     IS 'Informational work days for the month';
