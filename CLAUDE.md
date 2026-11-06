# CLAUDE.md — ReconcileIQ

## Project Summary
ReconcileIQ is a modern reconciliation product designed for D2C brands (like Company A, Bengaluru) to automatically ingest partner files (orders, settlements, COD remittances, freight invoices), run order-level matching against rate cards, surface payment shortfalls and unremitted COD, flag overcharged freight, and manage partner claims with audit trails.

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.4+, Maven, Spring Web, Spring Security (stateless JWT), Spring Data JPA, JdbcTemplate (batch/COPY), Flyway, Bean Validation, springdoc-openapi, Apache Commons CSV, FastExcel.
- **Database**: PostgreSQL 16.
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, TanStack Query v5, TanStack Table v8 (with virtualization), React Router v6, React Hook Form, Zod, Recharts, lucide-react.
- **Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL), Spring Boot Test, MockMvc, Vitest, React Testing Library.
- **DevOps**: Multi-stage Dockerfiles, Docker Compose, GitHub Actions CI.

## Folder Structure
```
/
├── backend/             # Spring Boot Maven application
│   ├── src/
│   │   ├── main/java/com/reconcileiq/
│   │   │   ├── auth/
│   │   │   ├── ingestion/
│   │   │   ├── mapping/
│   │   │   ├── matching/
│   │   │   ├── ratecard/
│   │   │   ├── claims/
│   │   │   ├── dashboard/
│   │   │   └── common/
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.yml
│   └── pom.xml
├── frontend/            # React + Vite application
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── api/
│   │   ├── hooks/
│   │   ├── types/
│   │   └── utils/
│   └── package.json
├── docs/                # Architecture, API design, assumptions, rules, benchmark
├── scripts/             # Benchmark & seed scripts
└── docker-compose.yml
```

## Build, Run, & Test Commands
- **Backend**:
  - Build: `cd backend && mvn clean package -DskipTests`
  - Test: `cd backend && mvn test`
  - Run locally: `cd backend && mvn spring-boot:run`
- **Frontend**:
  - Install: `cd frontend && npm install`
  - Build: `cd frontend && npm run build`
  - Test: `cd frontend && npm test`
  - Dev server: `cd frontend && npm run dev`
- **Docker Compose**:
  - Run full stack: `docker compose up --build`
  - Stop: `docker compose down -v`

## Coding Standards & Conventions
1. **Package by feature**: Keep domain components grouped in `auth`, `ingestion`, `mapping`, `matching`, `ratecard`, `claims`, `dashboard`, `common`.
2. **DTO separation**: Domain entities are never returned in controllers; map to explicit Request/Response DTOs.
3. **Role Enforcement**: Every backend controller endpoint must have explicit `@PreAuthorize("hasRole('...')")`.
4. **Error Handling**: Standard RFC-7807/custom JSON format `{timestamp, status, code, message, details}` via `@ControllerAdvice`. No raw stack traces.
5. **PII Masking**: Customer names, phones, addresses masked in API outputs and UI by default.
6. **No Placeholders**: Write complete, working implementations with no TODOs or dead code.
7. **Attribution**:
   - Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
   - PRs end with `🤖 Generated with [Claude Code](https://claude.com/claude-code)`
