# Business Rules & Reconciliation Engine Specification

This document encodes the deterministic matching rules used by the
ReconcileIQ core engine. It is the single source of truth for test cases,
review queue logic, and the reconcile API responses.

## 1. Glossary & Units
All amounts are in INR (Indian Rupees). Two decimals. Round half-up.
`Expected` = amount partner *should* have paid us net of fees.
`Received` = amount actually remitted / invoiced from partner feeds.
`Tolerance` = configurable absolute difference, default 10.00 INR.
Boundary rule: |difference| == 10.00 → within tolerance. |difference| >= 10.01 → flag.

Money at risk counts only shortfalls, not over-payments. TCS/TDS are shown
separately and never counted as leakage.

## 2. Rate Card Computation

Channel expected settlement:
```
expected_settlement = order_value
                      - (order_value * commission_pct / 100)
                      - fixed_fee
                      - max(order_value * collection_fee_pct / 100, min_collection_fee)
```
If order is returned → `expected_settlement` is negative using the same formula
applied to the return line (clawback semantics).

Courier expected freight:
```
expected_freight = weight_slab_rate(billed_weight_kg)
                   + (payment_type == COD ? cod_fee : 0)
                   + (delivery_status == RETURNED ? return_fee : 0)
```
Weight slab rate: first slab with `min_weight <= weight < max_weight`.
If weight exceeds highest slab, use highest slab rate.

## 3. Receiving & Aggregation

For a given order:
- `received_settlement` = sum of all SETTLEMENT net_amount rows for that order,
  after removing duplicates by `(reference_no, net_amount, settlement_date)`.
- `received_cod` = sum of all COD_REMITTANCE net_remitted rows for that order.
- `received_freight` = total_freight from the latest FREIGHT_INVOICE per order.

Multiple partial rows are merged. Duplicate rows are de-duped by reference/UTR.

## 4. Due Date Calculation

Due date = delivery_date + settlement_window_days

Settlement windows (configurable, seeded defaults):
- COD remittance: T+7
- Marketplace prepaid: T+10
- Website prepaid: T+10
- Quick-commerce: T+3

Delivery date source: `orders.order_date` (if delivered) else `orders.order_date`.
Age buckets for money at risk:
- 0–7, 8–15, 16–30, 31+ days since due date.

## 5. Status Precedence (one status per order)

Applied in strict order; first match wins:

1. **UNPAID_PAST_DUE**
   `received_settlement == 0` AND `today > due_date`
   → Money at risk = max(0, expected_settlement - received_settlement)

2. **SHORT_PAID**
   `|received_settlement - expected_settlement| > tolerance`
   AND `received_settlement < expected_settlement`
   → Money at risk = expected_settlement - received_settlement

3. **FREIGHT_OVERCHARGED**
   `|received_freight - expected_freight| > tolerance`
   AND `received_freight > expected_freight`
   Note: settlement can be fine. Freight diff is tracked separately so an order
   can show both settlement settled and freight overcharged, but money at risk is
   only counted once per order (see 6). Freight difference is reported in the
   order detail evidence.

4. **SETTLED**
   All other cases where |difference| <= tolerance.

Return not received is a P1 status that supersedes the above when a
WAREHOUSE_SCAN file exists and shows the parcel not received within X days.
Implementation in Phase 5.

## 6. Money at Risk Rules

Money at risk = `expected_settlement - received_settlement` if positive and the
order is SHORT_PAID or UNPAID_PAST_DUE. Freight overcharge is *not* included in
money at risk; it is reported as a separate claimable overcharge.

An order is never double-counted. Status precedence ensures one classification.

## 7. TCS / TDS Handling

TCS and TDS amounts appear in the settlement rows but are excluded from
leakage calculations. In the UI they are displayed in a separate tax panel with
partner-wise summary:
```
net_leakage = expected_settlement - received_settlement
tax_deducted = sum(tcs_amount + tds_amount)
```

## 8. Mapping & AI Boundaries

Matching and expected amounts are rule-based only. AI is limited to:
- Column mapping suggestions (header normalization, synonym dictionary, Jaro-Winkler)
- Candidate match suggestions for missing order IDs (AWB/amount/date similarity)

Suggested matches go to `review_queue` and are never applied automatically.
Mapping suggestions require explicit approval; each approval creates a new version
and exactly one version is active. Rollback re-activates a prior version.

## 9. Edge Cases & Worked Examples

### Example A — Brief sample 1 (ORD-W-204118)
Order value 1499, COD, delivered. Channel commission 6%, fixed 10, collection 1%
Assumed: delivery 2026-10-01, payment due T+7 (COD) → due 2026-10-08.

Channel rate card: 6% commission, 10 fixed, 1% collection min 20.
expected_settlement = 1499 - 89.94 - 10 - max(14.99,20) = 1499 - 89.94 -10 -20 = 1379.06
(Numbers differ from brief assumed 1409; this illustrates interpretation risk.)

Received = 0
Difference = -1379.06, |diff| > tolerance, due date passed today 2026-11-06 → UNPAID_PAST_DUE
Money at risk = 1379.06

Interpretation per A1: brief may be using Expected 1409, Received 0. Status remains
UNPAID_PAST_DUE regardless of exact expected number.

### Example B — Brief sample 2 (ORD-M-88120)
Order value 1199, Prepaid, delivered.
Expected 899, Received 862, Difference -37.
|difference| = 37 > 10 → SHORT_PAID
Money at risk = 37

### Example C — Brief sample 3 (ORD-W-204377)
Order value 2398, Prepaid, Returned to warehouse.
Expected -96, Received -142
Received more negative than expected → partner clawed back 46 too much.
Expected freight: return fee 50
Received freight: 96
Freight diff = 46, |diff| > 10 → FREIGHT_OVERCHARGED
Settlement diff: received -142 vs expected -96 → |46|>10 but received is more negative
per semantics this is an over-deduction → treated as freight overcharge.

### Example D — Boundary tolerance
Expected 1000, Received 990 → diff 10 → WITHIN tolerance → SETTLED
Expected 1000, Received 989.99 → diff 10.01 → SHORT_PAID

### Example E — Duplicate rows
Settlement rows:
S1 ref R001 net 500
S2 ref R001 net 500 (duplicate)
S3 ref R002 net 200
Received = 500 + 200 = 700, duplicate ignored.

### Example F — TCS separated
Expected 1000, Received 950, TCS 20, TDS 10
Leakage = 50 (not 80). Tax deducted = 30 shown separately.

## 10. Idempotency & Re-run
POST /api/matching/run is idempotent.
Re-running recomputes `order_reconciliation` from base feeds using
`INSERT ... ON CONFLICT (order_id) DO UPDATE`. Update timestamps only change when
a field value changes. Audit log records each run.

## 11. PII Masking
API responses mask customer_name to first letter + asterisks, phone to last 4
digits, address to first 3 chars + asterisks. Masking is applied at the DTO
mapping layer, never in persistence.

## 12. Audit Trail
All status changes, claim transitions, mapping approvals, and file uploads are
written to `audit_log`. Claim lifecycle events are recorded in `claim_events`.

## 13. Test Matrix Summary

| Scenario | Expected Status | Money at Risk |
|---|---|---|
| Nothing received, past due | UNPAID_PAST_DUE | Full expected |
| Short payment > tolerance | SHORT_PAID | Shortfall |
| Exact payment within tolerance | SETTLED | 0 |
| Freight overcharged > tolerance | FREIGHT_OVERCHARGED | 0 (reported separately) |
| Returned with over-clawback | FREIGHT_OVERCHARGED / SHORT_PAID | Per rule |
| Duplicate settlement rows | SETTLED / SHORT_PAID | De-duped |
| TCS present | SETTLED / SHORT_PAID | Tax excluded |

All formulas are covered by unit tests in `com.reconcileiq.matching`.