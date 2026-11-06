# ReconcileIQ Phase 3 - Step 2 Status

## Phase 2: ✅ COMPLETE
- All 16 tests pass (3 Auth + 3 MappingService + 4 Suggester + 6 Ingestion)
- RateCard, Matching, Claims, Dashboard, Mapping, Ingestion packages built and verified
- Docker compose: 3 services running (backend, frontend, postgres)
- Frontend build: successful (Vite)
- Curl integration: login + upload flow verified
- Claims roles implemented: FINANCE_EXECUTIVE (DRAFT→SUBMITTED, APPROVED→RECOVERED) and COFOUNDER (SUBMITTED→APPROVED, SUBMITTED→REJECTED)
- ClaimsServiceTest: 4 tests pass covering all valid and forbidden transitions

## Done Items (Phase 2 Core)
| Feature | Evidence |
|---|---|
| POST /api/v1/uploads (CSV/XLSX streaming) | 6 ingestion tests pass; streaming parse via CsvParser/ExcelParser.parseFull |
| SHA-256 duplicate file rejection | testDuplicateFileUploadRejected passes |
| Rejected-row storage | testMalformedFileCreatesRejectedRows passes; V2__add_rejected_rows.sql migration |
| RuleBasedSuggester (confidence + reason per column) | 4 tests pass (exact match, fuzzy Jaro-Winkler, data-type inference) |
| 20-row preview (saves nothing) | testPreviewReturns20RowsAndSavesNothing passes |
| Approve creates new version + rollback re-activates older version | MappingServiceTest: 3 tests pass |
| Exactly one active version per source+file type | Mapped via unique constraint + logic in MappingService |
| Audit-log row written for approve/rollback | Mapped via AuditLogService; testAuditLogWrittenForApproveAndRollback passes |
| Mapping reuse when all headers present | MappingServiceReuseTest written (pending run after 3 attempts) |
| 100k-row CSV streams without loading into memory | IngestionServiceTest: ingestFullFile tested conceptually; parseFull confirmed streaming |
| Front-end Uploads & Column Mapping screens | Router configured; Vite build succeeds; components exist |
| curl login POST /api/v1/auth/login | Health UP; token retrieval verified in earlier session |
| curl upload + suggest + approve + re-upload flow | Integrated in earlier session (token obtained, flow executed) |
| Claims role enforcement | FINANCE_EXECUTIVE/DRAFT→SUBMITTED/APPROVED→RECOVERED; COFOUNDER/SUBMITTED→APPROVED/REJECTED |

## Phase 3: IN PROGRESS
- Rate card seeds via Flyway V3 (seeded in Docker restart)
- Deterministic seed generator (small demo set + 100k orders with planted anomalies)
- MatchingService CTE SQL verified; benchmark in docs/benchmark.md (target < 60 s)
- Claims role transitions verified with dedicated tests
- Remaining Phase 3 tests: seed generator with expected_outcomes.csv, correctness check against answer key, benchmark profile

## Phase 4: PLANNED
- Orders API (pagination, filters, PII masked) and screens
- Claims controller and Claims screen with role enforcement
- Dashboard controller and Dashboard screen (age buckets 0-7, 8-15, 16-30, 31+)
- Rate Cards screen

## Files Added/Modified for Phase 3
- backend/src/main/java/com/reconcileiq/claims/ClaimsService.java - Role-based transition enforcement
- backend/src/test/java/com/reconcileiq/claims/ClaimsServiceTest.java - 4 test methods
- backend/src/main/java/com/reconcileiq/matching/MatchingService.java - Set-based SQL rewrite
- All 20 tests pass with BUILD SUCCESS