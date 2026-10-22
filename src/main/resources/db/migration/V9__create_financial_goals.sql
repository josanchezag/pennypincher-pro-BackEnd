-- V9__create_financial_goals.sql
CREATE TABLE financial_goals (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name                    VARCHAR(150)  NOT NULL,
    level                   VARCHAR(30)   NOT NULL,
    target_amount           NUMERIC(18,2) NOT NULL,
    monthly_investment      NUMERIC(18,2) NOT NULL,
    expected_annual_return  NUMERIC(5,4)  NOT NULL,
    target_years            INTEGER,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_financial_goals_name               CHECK (btrim(name) <> ''),
    CONSTRAINT ck_financial_goals_target_amount      CHECK (target_amount > 0),
    CONSTRAINT ck_financial_goals_monthly_investment CHECK (monthly_investment >= 0),
    CONSTRAINT ck_financial_goals_annual_return      CHECK (expected_annual_return > 0 AND expected_annual_return <= 1),
    CONSTRAINT ck_financial_goals_target_years       CHECK (target_years IS NULL OR target_years > 0),
    CONSTRAINT ck_financial_goals_level              CHECK (level IN
        ('FINANCIAL_SECURITY', 'FINANCIAL_INDEPENDENCE', 'FINANCIAL_FREEDOM', 'ABSOLUTE_FREEDOM'))
);

CREATE TRIGGER trg_financial_goals_updated_at
    BEFORE UPDATE ON financial_goals
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  financial_goals                        IS 'Financial independence milestones; hard deleted';
COMMENT ON COLUMN financial_goals.expected_annual_return IS 'Fraction, e.g. 0.1100 for 11 %';
