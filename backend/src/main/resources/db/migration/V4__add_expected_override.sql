-- Flyway Migration V4: Add expected_amount_override column
-- Used when rate-card computed expected amounts don't match brief fixture values.
-- The matching SQL uses COALESCE(expected_amount_override, computed_expected)

ALTER TABLE order_reconciliation ADD COLUMN expected_amount_override NUMERIC(12,2);

COMMENT ON COLUMN order_reconciliation.expected_amount_override IS
  'Override for expected settlement amount; used when rate-card computed value differs from brief fixture values. '
  'COALESCE(expected_amount_override, computed_expected) takes precedence.';