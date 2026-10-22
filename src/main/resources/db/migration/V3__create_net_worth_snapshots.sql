-- V3__create_net_worth_snapshots.sql
CREATE TABLE net_worth_snapshots (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    snapshot_date      DATE          NOT NULL,
    total_assets       NUMERIC(18,2) NOT NULL,
    total_liabilities  NUMERIC(18,2) NOT NULL,
    net_worth          NUMERIC(18,2) NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_net_worth_snapshots_user_month UNIQUE (user_id, snapshot_date),
    CONSTRAINT ck_net_worth_snapshots_first_day  CHECK (EXTRACT(DAY FROM snapshot_date) = 1),
    CONSTRAINT ck_net_worth_snapshots_totals     CHECK (total_assets >= 0 AND total_liabilities >= 0),
    CONSTRAINT ck_net_worth_snapshots_net_worth  CHECK (net_worth = total_assets - total_liabilities)
);

CREATE TRIGGER trg_net_worth_snapshots_updated_at
    BEFORE UPDATE ON net_worth_snapshots
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMENT ON TABLE  net_worth_snapshots               IS 'Monthly point-in-time net worth; recalculating a month updates its row';
COMMENT ON COLUMN net_worth_snapshots.snapshot_date IS 'Always the first day of the month';
