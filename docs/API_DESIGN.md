# API DESIGN — ReconcileIQ

Base path: `/api/v1`

## Auth & Session
- `POST /api/v1/auth/login`: Authenticate user, return JWT token and profile info.
- `GET /api/v1/auth/me`: Get current user details and permissions.

## Ingestion & Uploads
- `POST /api/v1/uploads`: Upload partner file (`multipart/form-data`, params: `sourceId`, `fileType`). Requires `FINANCE_EXECUTIVE`.
- `GET /api/v1/uploads`: List upload batches with status, row count, error count.
- `GET /api/v1/uploads/{id}`: Get upload batch details and rejected rows.

## Column Mapping & AI
- `GET /api/v1/mappings`: Get active column mappings per source and file type.
- `POST /api/v1/mappings/suggest`: Analyze sample 20 rows, return AI suggested mappings with confidence & reasons.
- `POST /api/v1/mappings/approve`: Approve and activate mapping version. Requires `FINANCE_EXECUTIVE`.
- `GET /api/v1/mappings/{sourceId}/{fileType}/history`: Get version history of mappings.
- `POST /api/v1/mappings/{id}/rollback`: Rollback to a specific mapping version. Requires `FINANCE_EXECUTIVE`.

## Rate Cards
- `GET /api/v1/ratecards/channels`: List channel rate cards.
- `POST /api/v1/ratecards/channels`: Create/update channel rate card. Requires `FINANCE_EXECUTIVE`.
- `GET /api/v1/ratecards/couriers`: List courier rate cards and weight slabs.
- `POST /api/v1/ratecards/couriers`: Create/update courier rate card & slabs. Requires `FINANCE_EXECUTIVE`.

## Matching & Reconciliation
- `POST /api/v1/matching/run`: Trigger bulk set-based matching engine. Requires `FINANCE_EXECUTIVE`.
- `GET /api/v1/orders`: Query orders with pagination, sorting, status/channel/date/difference filters. PII masked.
- `GET /api/v1/orders/{orderId}`: Order reconciliation detail view, full evidence calculation breakdown. PII masked.
- `GET /api/v1/review-queue`: List unmatched/missing order ID rows with candidate suggestions.
- `POST /api/v1/review-queue/{id}/resolve`: Manually match or ignore review queue item. Requires `FINANCE_EXECUTIVE`.

## Claims Management
- `GET /api/v1/claims`: List claims by status, partner, date.
- `POST /api/v1/claims`: Create claim draft from flagged orders. Requires `FINANCE_EXECUTIVE`.
- `GET /api/v1/claims/{id}`: Get claim details, items, and event history.
- `POST /api/v1/claims/{id}/transition`: State transitions:
  - `SUBMITTED` (Executive)
  - `APPROVED` / `REJECTED` (Co-founder only)
  - `RECOVERED` (Executive)
- `GET /api/v1/claims/{id}/export`: Download CSV evidence package for claim.

## Dashboard & Analytics
- `GET /api/v1/dashboard/summary`: High-level KPIs (auto-match rate, total leakage, open claims, money at risk).
- `GET /api/v1/dashboard/money-at-risk`: Money at risk matrix by partner and age bucket (0-7, 8-15, 16-30, 31+ days).
- `GET /api/v1/dashboard/tax-summary`: TCS and TDS breakdown by partner.
- `GET /api/v1/dashboard/leakage-breakdown`: Shortfall distribution by status (SHORT_PAID, UNPAID_PAST_DUE, FREIGHT_OVERCHARGED).
