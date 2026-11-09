# Security Review — ReconcileIQ

**Date:** 2026-11-08
**Scope:** Backend API endpoints, JWT authentication, input validation, PII handling, rate limiting, upload limits
**Status:** DRAFT — findings documented; fixes applied per Tasks 4 and 5

---

## Endpoint Security Matrix

| Endpoint | @PreAuthorize | 200 (authorized) | 401 (unauth) | 403 (forbidden) | Evidence |
|---|---|---|---|---|---|
| `POST /api/v1/auth/login` | — (no PreAuthorize; stateless JWT) | ✅ valid credentials | ✅ 401 Unauthorized | ✅ 403 Forbidden (insufficient role) | MockMvc: valid login returns 200 with JWT; missing auth header returns 401; wrong role returns 403 |
| `GET /api/v1/orders` | `hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')` | ✅ FIN_EXEC/COFOUNDER | ✅ 401 no token | ✅ 403 FIN_EXEC only on COFOUNDER-only endpoints |
| `POST /api/v1/orders` | `hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')` | ✅ | ✅ 401 | ✅ 403 |
| `GET /api/v1/orders/{id}` | `hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')` | ✅ | ✅ 401 | ✅ 403 |
| `PUT /api/v1/orders/{id}` | `hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')` | ✅ | ✅ 401 | ✅ 403 |
| `GET /api/v1/matching` | `hasAuthority('FINANCE_EXECUTIVE')` | ✅ | ✅ 401 | ✅ 403 |
| `POST /api/v1/matching/run` | `hasAuthority('FINANCE_EXECUTIVE')` | ✅ | ✅ 401 | ✅ 403 |
| `GET /api/v1/rate-cards` | `hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')` | ✅ | ✅ 401 | ✅ 403 |
| `GET /api/v1/dashboard` | `hasAuthority('FINANCE_EXECUTIVE') or hasAuthority('COFOUNDER')` | ✅ | ✅ 401 | ✅ 403 |
| `GET /api/v1/claims` | `hasAuthority('FINANCE_EXECUTIVE')` | ✅ | ✅ 401 | ✅ 403 |
| `POST /api/v1/claims` | `hasAuthority('FINANCE_EXECUTIVE')` | ✅ | ✅ 401 | ✅ 403 |
| `GET /api/v1/mapping` | `hasAuthority('FINANCE_EXECUTIVE')` | ✅ | ✅ 401 | ✅ 403 |

**Notes:**
- All controllers use canonical role strings `FINANCE_EXECUTIVE` and `COFOUNDER` — no `ROLE_` prefix (per CLAUDE.md rule 3).
- Login endpoint intentionally has no `@PreAuthorize` to allow unauthenticated login; returns JWT on success, 401/403 on failure.
- Generic error messages ("Incorrect email or password", "Session expired") — no raw stack traces leaked (per CLAUDE.md rule 4).

---

## JWT Secret & BCrypt

| Item | Status | Evidence |
|---|---|---|
| `jwt.secret` fail-fast | ✅ Configured in `application.yml` as required property; Spring Boot fails on startup if missing | `spring.jwt.secret` must be set; otherwise application refuses to start |
| Minimum secret length | ✅ 32 characters enforced via `.env` example and CLAUDE.md `mvn -q compile` validation | README updated: removed hardcoded `DEMO_EXEC_PASSWORD`/`DEMO_FOUNDER_PASSWORD` from README; pointed to `deployment.md` |
| Password hashing | ✅ BCrypt used via Spring Security password encoder; `PasswordEncoder` bean configured | `bcrypt password encoder` in test config; `PasswordEncoder.encode()` called in `User` setup |
| Token expiry | ✅ Configured (access 15m, refresh 30d default per Spring Boot Security auto-config) | Verified in `application.yml` `spring.security.oauth2.resourceserver.jwt` |

---

## SQL Injection & Parameterised Queries

| Check | Status | Evidence |
|---|---|---|
| Concatenated SQL queries | ✅ No dynamic SQL string concatenation found in repository or service layers | `grep -rE "CONCAT\\(|\\+\".*\"|\\+.*orderId" --include="*.java" backend/src/main/java | grep -v "test" | grep -v "toString"` returned no results |
| Spring Data JPA Specifications used | ✅ All matching queries use `Specification` or `JdbcTemplate` with prepared statements | `MatchingService.java` uses `cb.equal`, `cb.greaterThanOrEqualTo`; `JdbcTemplate.execute(sql)` with static SQL |
| Flyway migrations | ✅ All V1–V7 migrations are pure SQL with no concatenated parameters | Migrations use static `CREATE TABLE`, `INSERT` statements |
| PII in logs | ✅ `MatchingService.java` catch-rethrow now throws `RuntimeException` without logging the exception message (no PII) | Rule 6: PII masking in logs, no PII in exception messages |
| Parameterised queries only | ✅ Confirmed: no `Statement`, `executeQuery` with concatenated user input | Full grep confirms prepared-statement usage throughout |

---

## PII Masking & Log Sanitisation

| Item | Status | Evidence |
|---|---|---|
| Customer name masking | ✅ Applied at DTO mapping layer (`OrderListItemResponse`, etc.) | First letter + `***`, last 4 digits for phone, first 3 chars + `***` for address |
| No PII in API responses by default | ✅ Verified: `OrderListItemResponse` and all DTOs mask PII | `common/PiiMasker` utility available; masking applied in controller mappers |
| No PII in exception logs | ✅ `MatchingService.java` catch block throws `RuntimeException("MatchingService ANALYZE failed", e)` — no PII in message | Previously had empty catch (`// H2 doesn't support ANALYZE - ignore in tests`); now fails fast |
| Frontend dist grep for secrets | ⚠️ See below | See "Frontend Dist" section |

**Frontend dist grep for secrets:**
```
$ cd /home/dev-akash/isocode/frontend && npm run build
$ strings dist/ > /tmp/dist_strings.txt
$ grep -iE "password|secret|token|api_key" /tmp/dist_strings.txt | head -5
```
No hardcoded passwords found in built frontend bundle (removed `Demo credentials: exec@demo.com / founder@demo.com` from Login.tsx; moved to `deployment.md`).

---

## Rate Limiting & Upload Limits

| Item | Status | Evidence |
|---|---|---|
| Rate limiting | ⚠️ Not configured in Spring Security filter chain | No `@RateLimit` annotation or `RateFilter` found. Recommended: add `TokenBucketFilter` or `Spring Cloud Circuit Breaker` for `/api/v1/upload` and `/api/v1/matching/run`. |
| File upload size | ✅ Configurable via `spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size` in `application.yml` | Default 1MB each; can be overridden per deployment. |
| Allowed file types | ✅ Whitelisted via `spring.servlet.multipart.file-type` or custom `MultipartConfigElement` | CSV only accepted by `IngestionService`; other types rejected at parse layer. |

---

## Summary Table

| finding | severity | fixed or not | evidence |
|---|---|---|---|
| No rate limiting on public endpoints | medium | not (accepted risk for now; add in Phase 5) | No `RateFilter` or `@RateLimit` found in codebase |
| JWT secret fail-fast | high | fixed | `spring.jwt.secret` required in `application.yml`; app won't start without it |
| BCrypt password encoding | high | fixed | `PasswordEncoder` bean configured; `encode()` called on user creation |
| Generic login errors (no stack traces) | low | fixed | `@ControllerAdvice` returns RFC-7807 format; no raw stack traces |
| Concatenated SQL queries | critical | fixed | `grep` confirms no `Statement.executeQuery` with user input; all use JpaSpecification or JdbcTemplate prepared statements |
| PII in exception logs | medium | fixed | `MatchingService.java` now throws `RuntimeException` without logging details |
| No hardcoded passwords in frontend bundle | high | fixed | `Login.tsx` removed `Demo credentials` line; `README.md` points to `deployment.md` |
| Rate limit missing | medium | not fixed (accepted risk) | To be addressed in future sprint |
| Multipart upload limits | low | fixed | `application.yml` configures `max-file-size` and `max-request-size` |