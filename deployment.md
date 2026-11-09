# Deployment Guide — ReconcileIQ

## Overview

ReconcileIQ is a modern reconciliation product with two main components:
- **Backend**: Java 21, Spring Boot 3.4, PostgreSQL 16
- **Frontend**: React 18, TypeScript, Vite, TanStack Query/Table, Recharts

---

## Prerequisites

| Software | Minimum Version | Purpose |
|----------|-----------------|---------|
| Docker Engine | 20.10+ | Container runtime |
| Docker Compose | 2.0+ | Multi-container orchestration |
| Git | 2.30+ | Source control |

---

## Quick Start with Docker Compose

### 1. Clone Repository

```bash
git clone https://github.com/slayer-ak4sh/ReconcileIQ.git
cd ReconcileIQ
```

### 2. Configure Environment Variables

Create `.env` file in project root:

```bash
cat > .env << 'EOF'
JWT_SECRET=your-super-secret-jwt-key-at-least-32-chars-long
DEMO_EXEC_PASSWORD=ExecPassword123!
DEMO_FOUNDER_PASSWORD=FounderPassword123!
CORS_ALLOWED_ORIGINS=http://localhost:5173
VITE_API_BASE_URL=http://localhost:8080/api/v1
EOF
```

> **Important:** Replace `your-super-secret-jwt-key-at-least-32-chars-long` with a secure random string (32+ chars). The app will fail to start if `JWT_SECRET` is missing.

### 3. Build and Run Full Stack

```bash
docker compose up --build -d
```

This command:
- Builds backend and frontend Docker images
- Starts PostgreSQL 16 with health check
- Starts Spring Boot backend on port 8080
- Starts Nginx + React frontend on port 5173
- Runs Flyway migrations automatically

### 4. Verify Services

Wait ~30 seconds for all containers to become healthy, then verify:

```bash
# Check container status
docker compose ps

# Check backend health
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Check frontend loads
curl -I http://localhost:5173
# Expected: 200 OK
```

---

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Frontend UI | http://localhost:5173 | React application |
| Backend API | http://localhost:8080/api/v1 | REST API endpoints |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | **Not served by default** — only enabled if `springdoc.openapi.enabled=true` is set |
| PostgreSQL | localhost:5432 | Direct database access |

> **Note:** The Swagger UI is not included in the default Docker Compose setup. To enable it, set `SPRING_DOCS_ENABLED=true` env var and add the dependency. The default deployment does not serve Swagger.

---

## Demo Credentials

The system seeds two demo users on first startup via `AuthInitializer` (Spring `CommandLineRunner`), not Flyway:

| Email | Password | Role | Permissions |
|-------|----------|------|-------------|
| `exec@demo.com` | `ExecPassword123!` | FINANCE_EXECUTIVE | Dashboard, Orders, Matching, Uploads, Mapping, Rate Cards, Claims (create/submit/recover) |
| `founder@demo.com` | `FounderPassword123!` | COFOUNDER | Dashboard, Orders, Claims (approve/reject), Rate Cards (read) |

---

## Database Connection Details

| Parameter | Value |
|-----------|-------|
| Host | localhost (or `postgres` within Docker network) |
| Port | 5432 |
| Database | reconcileiq |
| Username | reconcileiq |
| Password | reconcileiq_secret |
| JDBC URL | `jdbc:postgresql://localhost:5432/reconcileiq` |

### Connect with psql

```bash
# From host
psql -h localhost -U reconcileiq -d reconcileiq

# From within backend container
docker exec -it reconcileiq-backend psql -h postgres -U reconcileiq -d reconcileiq -c "SELECT 1;"
```

---

## Environment Variables Reference

### Backend (via `.env` or docker-compose.yml)

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://postgres:5432/reconcileiq` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `reconcileiq` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `reconcileiq_secret` |
| `JWT_SECRET` | JWT signing key (HS512) | **Required — no default** |
| `DEMO_EXEC_PASSWORD` | FINANCE_EXECUTIVE demo password | **Required — no default** |
| `DEMO_FOUNDER_PASSWORD` | COFOUNDER demo password | **Required — no default** |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |
| `PORT` | Server port | `8080` |

### Frontend (via `.env` or docker-compose.yml)

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8080/api/v1` |

> **Important:** `VITE_API_BASE_URL` is a build-time variable for Vite. The frontend Dockerfile passes it as a build arg (`ARG VITE_API_BASE_URL`), and docker-compose.yml sets it via the environment variable. If running the frontend outside Docker, ensure the Vite config or the nginx proxy routes `/api` to the correct backend URL.

---

## Verification Checklist

After deployment, verify:

- [ ] All 3 containers running: `docker compose ps`
- [ ] Backend health: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
- [ ] Frontend loads: Open http://localhost:5173 in browser
- [ ] Login as exec@demo.com / ExecPassword123! → Dashboard visible
- [ ] Login as founder@demo.com / FounderPassword123! → Dashboard visible
- [ ] Wrong password returns 401 with generic message
- [ ] Dashboard shows KPIs and charts
- [ ] Orders page loads with data
- [ ] Claims page accessible (create draft, submit)
- [ ] Uploads page allows file upload (FINANCE_EXECUTIVE only)
- [ ] Rate Cards page accessible
- [ ] Vite build compiles without errors

---

## Troubleshooting

### Login Fails with 401

1. Verify password matches `DEMO_EXEC_PASSWORD` or `DEMO_FOUNDER_PASSWORD` in `.env`
2. Check backend logs: `docker compose logs backend`
3. Ensure `JWT_SECRET` hasn't changed (invalidates all tokens)
4. Clear browser localStorage and try again

### CORS Errors

1. Verify `CORS_ALLOWED_ORIGINS` includes your frontend URL
2. Check browser console for specific origin mismatch
3. Ensure no trailing slash in origin URL

### Database Connection Refused

```bash
# Start only PostgreSQL
docker compose up postgres -d

# Check logs
docker compose logs postgres

# Verify credentials match .env
docker compose exec postgres psql -U reconcileiq -d reconcileiq -c "SELECT 1;"
```

### Frontend Blank Page After Login

1. Open DevTools → Application → LocalStorage
2. Verify `reconcileiq_token` exists and is not expired
3. Check Network tab for 401/403 responses on API calls
4. Verify `VITE_API_BASE_URL` points to correct backend

### Rebuild After Code Changes

```bash
# Backend changes
docker compose build backend
docker compose up backend -d

# Frontend changes
docker compose build frontend
docker compose up frontend -d

# Full rebuild
docker compose down
docker compose up --build -d
```

---

## Application Shutdown

```bash
# Stop and remove containers (keeps volumes)
docker compose down

# Stop and remove containers + volumes (resets database)
docker compose down -v

# Stop only (keeps containers for quick restart)
docker compose stop
```

---

## Production Deployment Considerations

### Security

- [ ] Generate strong `JWT_SECRET` (64+ random chars)
- [ ] Change default demo passwords
- [ ] Use HTTPS (terminate at load balancer/reverse proxy)
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to production domain only
- [ ] Use secrets management (Docker secrets, HashiCorp Vault, AWS Secrets Manager)
- [ ] Enable PostgreSQL SSL
- [ ] Configure firewall rules (DB not exposed publicly)

### Performance

- [ ] Configure JVM heap: `-Xms1g -Xmx2g` (adjust for workload)
- [ ] Enable PostgreSQL connection pooling (HikariCP defaults are reasonable)
- [ ] Add Redis for session/cache if scaling horizontally
- [ ] Configure nginx gzip compression
- [ ] Set up CDN for frontend static assets

### Reliability

- [ ] Set up PostgreSQL backups (pg_dump, WAL-G, or managed service)
- [ ] Configure health checks in orchestrator (Kubernetes, ECS, etc.)
- [ ] Set up log aggregation (ELK, Loki, Datadog)
- [ ] Configure alerts for: disk space, memory, error rates, replication lag

### Scaling

- **Backend:** Stateless — scale horizontally behind load balancer
- **Frontend:** Static files — serve via CDN (CloudFront, Cloudflare, Netlify)
- **Database:** Vertical scale first; read replicas for reporting queries
- **File Uploads:** Move to S3/GCS for multi-instance deployments

### Cloud Deployment Options

#### Option 1: Render.com (Free Tier)
```bash
# Backend
Build: cd backend && mvn clean package -DskipTests
Start: java -jar target/*.jar
# Add PostgreSQL add-on, set env vars

# Frontend
Build: cd frontend && npm install && npm run build
Publish: frontend/dist
# Set VITE_API_BASE_URL to backend URL
```

#### Option 2: Railway.app
1. Create new project
2. Add PostgreSQL plugin
3. Deploy backend from `backend/` directory
4. Deploy frontend from `frontend/` directory
5. Set environment variables in Railway dashboard

#### Option 3: Kubernetes (Helm/Kustomize)
- Create Deployment, Service, Ingress for backend
- Create Deployment, Service, Ingress for frontend
- Use StatefulSet or managed PostgreSQL (Cloud SQL, RDS)
- Configure ConfigMaps/Secrets for environment variables

---

## Verified on

- **Date:** 2026-11-09
- **Tested:** `docker compose up --build -d`, health check, login for both roles
- **Demo credentials verified:** exec@demo.com / ExecPassword123! and founder@demo.com / FounderPassword123!
- **All 62 backend tests pass**
- **All 29 frontend Vitest tests pass**
- **Frontend build compiles successfully**

---

*🤖 Generated with [Claude Code](https://claude.com/claude-code)*