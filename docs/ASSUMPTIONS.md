# Assumptions

Every assumption below is a labeled decision made by the builder team because the
brief was silent or ambiguous. None of them invent client requirements. Confirm
each with the client before relying on it in production.

## A1 — Reading the ambiguous sample row
The brief's first sample row is written as:

`ORD-W-204118 | Website | 1499 | COD | Delivered | 1409 | 0 | -1409`

The final three columns are labeled Expected / Received / Difference. The most
consistent reading is **Expected = 1409, Received = 0, Difference = -1409**
(i.e. the order was delivered but nothing was remitted). That is the
interpretation this product implements.

Alternative readings the brief does not rule out:
- Expected = 1409, Received = 1409, Difference = -1409 (difference column is
  simply wrong in the source).
- The `0` is the COD remittance and `1409` is something else.

**Status: interpretation of ambiguous sample row; confirm against original brief.**
If the client confirms a different reading, only the seed row and the
`docs/rules.md` worked example change; the matching engine is unaffected.

## A2 — Negative amounts are clawbacks
Returned orders carry negative expected and received amounts.
`ORD-W-204377`: expected -96, received -142 means the partner clawed back 46
more than it should have. A negative difference (received more negative than
expected) is therefore **FREIGHT_OVERCHARGED**, not a credit.

## A3 — File schemas are synthetic
The four core file types use deliberately different column names per partner so
the mapping engine is exercised:
- Settlement: 2 channels (Website, Marketplace 1), each with its own header set.
- COD remittance: 2 couriers, each with its own header set.
- Freight invoice: 2 couriers, each with its own header set.
- Warehouse scan (P1): 1 format.

Canonical field names are fixed in the code (`CanonicalField` enum); partner
headers are mapped onto them.

## A4 — Due date rule
Due date = delivery date + settlement window, configurable per channel and
payment type. Seeded values:
- COD remittance: T+7
- Marketplace prepaid: T+10
- Website prepaid: T+10
- Quick-commerce: T+3
Stored in `settings`, never hardcoded.

## A5 — Rate cards are data, not code
Channel rate card = commission %, fixed fee, collection fee.
Courier rate card = weight slabs (rate per slab), COD fee, return fee.
All stored in the database. No rate or fee constant appears in business logic.

## A6 — Tolerance
Absolute difference of exactly Rs 10.00 is **within** tolerance.
Rs 10.01 is flagged. Tolerance value is a configurable setting (default 10.00).

## A7 — TCS and TDS
TCS and TDS percentages are configurable settings. No statutory rates are
hardcoded. They are stored in separate columns and are **never** counted as
leakage or money at risk.

## A8 — Roles
- `FINANCE_EXECUTIVE`: upload, mappings, rate cards, matching, claims
  (DRAFT → SUBMITTED, APPROVED → RECOVERED), review queue.
- `COFOUNDER`: read-only dashboards, orders, claims; approve/reject claims only.

## A9 — Warehouse scan file
A fifth file type (`WAREHOUSE_SCAN`) exists for the returns tracker. P1 only.

## A10 — Demo credentials
`exec@demo.com` / `founder@demo.com`. Passwords come from env vars
`DEMO_EXEC_PASSWORD` and `DEMO_FOUNDER_PASSWORD`, with documented demo defaults
used **only** in the `demo` Spring profile.

## A11 — Money at risk excludes tax
Money at risk counts only net shortfalls (excluding TCS/TDS). An order with a
Rs 100 shortfall and Rs 20 TCS deducted shows Rs 100 at risk and Rs 20
reported separately as tax.

## A12 — "Expected" in the sample data
In the brief's sample table, "Expected" is the **expected settlement net amount**
(order value minus channel fees), and "Received" is what the partner actually
remitted. This matches the business rule in `docs/rules.md`.

## A13 — Seeded demo month
The demo dataset covers one calendar month (the current month) so a reviewer
logging in sees a coherent, recent picture rather than stale history.

## A14 — INR formatting
All currency is rendered in INR using `en-IN` numbering with the ₹ symbol,
right-aligned in tables.

## A15 — No real money movement
Claims are records of identified leakage. Nothing in this product initiates
payments, refunds or filings with partners or banks. "Recovered" is a
finance-confirmed status, not a transaction.