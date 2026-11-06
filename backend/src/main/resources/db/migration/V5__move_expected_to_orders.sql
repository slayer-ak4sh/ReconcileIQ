-- Flyway Migration V5: Move expected_amount_override from order_reconciliation to orders
-- This allows it to be an INPUT on orders, not on order_reconciliation

-- Step 1: Add expected_amount_override column to orders table
ALTER TABLE orders ADD COLUMN expected_amount_override NUMERIC(12,2);

-- Step 2: Copy existing values from order_reconciliation to orders (if any)
UPDATE orders o SET expected_amount_override = (
    SELECT oo.expected_amount_override FROM order_reconciliation oo WHERE oo.order_id = o.order_id
);

-- Step 3: Drop the column from order_reconciliation
ALTER TABLE order_reconciliation DROP COLUMN IF EXISTS expected_amount_override;

-- Step 4: Add comment on the new column
COMMENT ON COLUMN orders.expected_amount_override IS
  'Override for expected settlement amount per order; used when rate-card computed value differs from brief fixture values. '
  'Used by MatchingService as a override before computing from rate cards.';