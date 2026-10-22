-- V10__create_indexes.sql
-- Every user_id foreign key is the leading column of an index. net_worth_snapshots and
-- monthly_budgets already get one from their (user_id, ...) unique constraints.
CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE INDEX idx_assets_user_id_active            ON assets (user_id, is_active);
CREATE INDEX idx_liabilities_user_id_active       ON liabilities (user_id, is_active);
CREATE INDEX idx_income_sources_user_id_active    ON income_sources (user_id, is_active);
CREATE INDEX idx_credit_cards_user_id_active      ON credit_cards (user_id, is_active);
CREATE INDEX idx_loans_user_id_active             ON loans (user_id, is_active);
CREATE INDEX idx_fixed_expenses_user_id_active    ON fixed_expenses (user_id, is_active);
CREATE INDEX idx_variable_expenses_user_id_active ON variable_expenses (user_id, is_active);
CREATE INDEX idx_savings_accounts_user_id_active  ON savings_accounts (user_id, is_active);
CREATE INDEX idx_financial_goals_user_id          ON financial_goals (user_id);
