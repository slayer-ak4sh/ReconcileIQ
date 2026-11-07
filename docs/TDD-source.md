# TDD Source Documentation

*Live URL: placeholder*
*Repo URL: placeholder*

## Executive Summary

This document describes the Test-Driven Development approach used to build ReconcileIQ, the implemented tests, and the current state of coverage.

## What Is Built

### 1. Authentication & Authorization (TDD)

**Source**: Test-first development using Spring Security MockMvc tests.

- `AuthControllerTest`: Tests login with valid/invalid credentials, token generation
- `AuthInitializer`: Creates demo users on startup
- Role-based access using `@PreAuthorize("hasAuthority('...')")`

**Tests Implemented**:
- exec@demo.com login succeeds → 200 with token
- founder@demo.com login succeeds → 200 with token  
- Wrong password → 401 Invalid credentials
- Unknown email → 401 Invalid credentials
- Unauthenticated API calls → 401 Unauthorized
- Role-based endpoint access (FINANCE_EXECUTIVE vs COFOUNDER)

### 2. Orders API (TDD)

**Source**: `OrdersControllerTest`, `OrdersController`

- Pagination: `page`, `size`, `sortBy`, `sortDir` params
- Filtering by partner, status, date range
- PII masking (customer name, phone, address)
- 401 for unauthenticated users

**Tests**: GET list paginated, GET detail, 401 unauthenticated, PII masked

### 3. Dashboard API (TDD)

**Source**: `DashboardControllerTest`, `DashboardController`

- `/api/v1/dashboard/summary` - aggregate metrics
- `/api/v1/dashboard/money-at-risk` - by partner and age buckets

**Tests**: Summary metrics, money-at-risk, 401 unauthenticated

### 4. Claims API (TDD)

**Source**: `ClaimsControllerTest`, `ClaimsServiceTest`, `ClaimsController`, `ClaimsService`

Workflow: Create Draft → Submit → Approve/Reject → Recover

**Role Matrix**:
- FINANCE_EXECUTIVE: create, submit, recover
- COFOUNDER: approve, reject only

**Tests**: 6 Controller tests, 11 Service tests covering full lifecycle

### 5. Rate Cards API (TDD)

**Source**: `RateCardsControllerTest`, `RateCardsController`

- Channel rate cards: commission, fixed fee, collection fee
- Courier rate cards: weight slabs, COD fee, return fee

**Tests**: GET all, GET by channel, PUT updates, role enforcement

### 6. Ingestion API (TDD)

**Source**: `IngestionServiceTest`, `IngestionController`

- CSV and Excel file upload
- Column mapping suggestions
- Data streaming with batch inserts

**Tests**: File upload parsing, duplicate detection, mapping

### 7. Matching Engine

**Source**: `MatchingService`, Matching SQL queries

- PostgreSQL set-based CTE queries
- Status precedence: UNPAID_PAST_DUE, SHORT_PAID, FREIGHT_OVERCHARGED, PENDING, SETTLED
- Deduplication of settlements
- Money at risk calculation (excludes TCS/TDS)

**Tests**: Integration tests verify matching logic

## What Is NOT Built

### Excluded Features

1. **User Sign Up**: No registration endpoint. Demo users auto-created.
2. **File Attachments**: Claims cannot have uploaded evidence files.
3. **LLM Sugester**: No AI-powered matching suggestions.
4. **Multi-currency**: All amounts in INR.
5. **Email Notifications**: No email/webhook notifications for claims.
6. **Bulk Export**: No CSV export for claims or reconciliations.
7. **Advanced Filtering**: Claims API has basic filtering only.

### Known Limitations

1. Matching requires all four data sources present
2. Rate card lookup assumes single effective record per entity
3. No webhook callbacks for real-time updates
4. CORS restricted to localhost in development

## Test Results

```
Tests run: 51, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Coverage Gaps

| Component | Coverage | Notes |
|-----------|----------|-------|
| MatchingService | Integration tests | Full end-to-end via SQL |
| Claims Flow | Full | DRAFT→SUBMITTED→APPROVED→REJECTED→RECOVERED |
| Role Enforcement | Full | 403 for unauthorized role access |
| Error Handling | Partial | 401/403 covered, 500 errors logged |

## Future Improvements

1. Add unit tests for MatchingService.java methods
2. Add frontend component tests (Vitest coverage needs work)
3. Add contract tests for API stability
4. Add end-to-end tests with Cypress/Puppeteer