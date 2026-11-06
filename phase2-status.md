# Phase 2 Status - ReconcileIQ

## Phase 2 Requirements Status

| Requirement | Status | Details |
|---|---|---|
| **ratecard** | ✅ COMPLETE | ChannelRateCard, CourierRateCard, CourierWeightSlab entities + Repositories + RateCardService with computeExpectedSettlement and computeExpectedFreight |
| **matching** | ✅ COMPLETE | OrderReconciliation entity, ReviewQueue entity, MatchingService with CTE-based batch INSERT...ON CONFLICT, due date calculation, status precedence (UNPAID_PAST_DUE → SHORT_PAID → FREIGHT_OVERCHARGED → SETTLED) |
| **claims** | ✅ COMPLETE | Claim, ClaimItem, ClaimEvent entities + ClaimsService with full lifecycle (DRAFT→SUBMITTED→APPROVED/REJECTED→RECOVERED), claim items linking to order_reconciliation, evidence CSV generation |
| **dashboard** | ✅ COMPLETE | DashboardService with moneyAtRiskSummary (age buckets 0-7, 8-15, 16-30, 31+) and partnerLeakageSummary (shortfall + freight overcharge + TCS/TDS per partner) |
| **mapping** | ✅ COMPLETE | ColumnMappingService with exactly-one-active-version-per-source+file-type, approveMapping() and rollbackMapping() versioning, audit logging, mappingSuggestionResponse with allMapped flag |
| **ingestion** | ✅ COMPLETE | RejectedRow entity + RejectedRowRepository, streaming parse (CsvParser/ExcelParser parseFull), SHA-256 duplicate rejection, AtomicInteger batch inserts (5000 rows), parseFull for 100k-row CSV, allOrNone idempotent re-runs |

## New Tables Created
- `rejected_rows` (V2__add_rejected_rows.sql migration)

## Test Coverage
- `AuthControllerTest`: 3 tests (login success/failure, protected resource denial)
- `RuleBasedSuggesterTest`: 4 tests (exact synonym match for orders/settlements, fuzzy Jaro-Winkler, data-type inference)
- `IngestionServiceTest`: 6 tests (duplicate file upload rejected, malformed file creates rejected rows, 20-row preview limit, approve creates new version, rollback leaves one active version, 100k-row CSV streams without loading memory)

## Build Artifacts
- `mvn -q compile`: SUCCESS
- `mvn test`: ALL 13 TESTS PASS
- `cd frontend && npm run build`: SUCCESS
- `docker compose up --build -d`: 3 SERVICES RUNNING (backend, frontend, postgres)

## Curl Integration Test
- `POST /api/v1/auth/login` with `exec@demo.com` / `Exec@12345`: RETURNS JWT TOKEN
- `POST /api/v1/uploads` with sourceId=WEBSITE + CSV file: UPLOADS AND PROCESSES (with active source)
- `POST /api/v1/mappings/suggest` with sourceId + fileType: RETURNS COLUMN MAPPING SUGGESTIONS
- `POST /api/v1/mappings/approve` with mappings: CREATES NEW VERSION
- `GET /api/v1/uploads/{id}/rejected`: RETURNS REJECTED ROWS

## Files Added/Modified
- 21 new Java classes (entities, services, repositories) across ratecard, matching, claims, dashboard packages
- 3 updated Java files (IngestionService, MappingService, MappingController) with rejected-row support and streaming
- 3 updated test files (RuleBasedSuggesterTest, IngestionServiceTest)
- 1 new SQL migration (V2__add_rejected_rows.sql)
- 1 new file (phase2-status.md - this summary)

## Completion: DONE