# ReconcileIQ

ReconcileIQ is a modern, high-performance reconciliation engine for D2C brands. It automates the matching of orders against partner settlements, COD remittances, and freight invoices to surface payment shortfalls and overcharged freight.

## Status
- **Phase 0 (Plan & Foundation)**: ✅ Complete.
- **Phase 1 (Foundation)**: 🛠️ In Progress. Skeleton backend and frontend are ready.

## Key Features
- **Deterministic Engine**: Rule-based matching using PostgreSQL CTEs for 100k-row performance.
- **AI Mapping**: Normalization and suggestion of column headers with human-in-the-loop approval.
- **Evidence Trail**: Detailed order-level breakdown showing every fee and deduction.
- **Claims Workflow**: Integrated claim lifecycle from identification to recovery.
- **PII Masking**: Compliant with DPDP principles.

## Quick Start (Docker)

1. Create a `.env` file with a secret JWT key:
   ```bash
   JWT_SECRET=some-very-secret-key-at-least-32-chars
   DEMO_EXEC_PASSWORD=Exec@12345
   DEMO_FOUNDER_PASSWORD=Founder@12345
   ```

2. Run the stack:
   ```bash
   docker compose up --build
   ```

3. Access the app:
   - Frontend: `http://localhost:5173`
   - API Docs: `http://localhost:8080/swagger-ui/index.html`
   - Default login: `exec@demo.com` / `Exec@12345`

## Tech Stack
- **Backend**: Java 21, Spring Boot 3.4, PostgreSQL 16, Flyway, JWT.
- **Frontend**: React 18, Vite, Tailwind CSS, TanStack Query/Table, Lucide.

## Development Commands
- Backend: `cd backend && mvn spring-boot:run`
- Frontend: `cd frontend && npm install && npm run dev`
- Tests: `cd backend && mvn test` | `cd frontend && npm test`

## Documentation
- [Brief](docs/BRIEF.md)
- [Architecture](docs/ARCHITECTURE.md)
- [API Design](docs/API_DESIGN.md)
- [Business Rules](docs/rules.md)
- [Performance Benchmark](docs/benchmark.md)
- [Assumptions](docs/ASSUMPTIONS.md)

---
Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
🤖 Generated with [Claude Code](https://claude.com/claude-code)
