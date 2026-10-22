-- V11__add_monthly_budget_component_totals.sql
-- Keeps the monthly amount of each budget component, so a budget shows the amounts of its last calculation.
ALTER TABLE monthly_budgets
    ADD COLUMN credit_card_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN loan_total        NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN expense_total     NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN savings_total     NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN charity_total     NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_monthly_budgets_component_totals CHECK (
        credit_card_total >= 0 AND loan_total >= 0 AND expense_total >= 0
        AND savings_total >= 0 AND charity_total >= 0);

COMMENT ON COLUMN monthly_budgets.credit_card_total IS 'Monthly credit card payments at the last calculation';
COMMENT ON COLUMN monthly_budgets.loan_total        IS 'Monthly loan installments at the last calculation';
COMMENT ON COLUMN monthly_budgets.expense_total     IS 'Fixed (except charity) plus variable expenses at the last calculation';
COMMENT ON COLUMN monthly_budgets.savings_total     IS 'Monthly savings contributions at the last calculation (not an expense)';
COMMENT ON COLUMN monthly_budgets.charity_total     IS 'Fixed expenses of the CHARITY category at the last calculation';
