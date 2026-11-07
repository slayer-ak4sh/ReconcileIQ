# Deployment Guide

## Overview

ReconcileIQ is a modern reconciliation product with two main components:
- **Backend**: Java 21, Spring Boot 3.4, PostgreSQL
- **Frontend**: React 18, TypeScript, Vite

## Prerequisites

- Docker Engine 20.10+
- GitHub account (for CI/CD)

## Quick Start with Docker Compose

### 1. Clone and Build

```bash
git clone <repo-url>
cd isocode
docker compose build
```

### 2. Run Full Stack

```bash
docker compose up -d
```

Services:
- Backend: http://localhost:8080
- Frontend: http://localhost:5173 (or http://localhost:80 for prod)
- PostgreSQL: localhost:5432

### 3. Initialize Database

The Flyway migrations run automatically on startup. Demo users are created automatically:
- `exec@demo.com` / `ExecPassword123!` (FINANCE_EXECUTIVE)
- `founder@demo.com` / `FounderPassword123!` (COFOUNDER)

## Environment Variables

### Backend (.env or docker-compose.yml)

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/reconcileiq` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `reconcileiq` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `reconcileiq_secret` |
| `JWT_SECRET` | JWT signing key | (auto-generated) |
| `DEMO_EXEC_PASSWORD` | Exec demo password | `ExecPassword123!` |
| `DEMO_FOUNDER_PASSWORD` | Founder demo password | `FounderPassword123!` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:5173` |

### Frontend (.env)

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API URL | `http://localhost:8080/api/v1` |

## Cloud Deployment Options

### Option 1: Render.com (Free Tier)

**Backend:**
1. Create new Web Service
2. Build command: `cd backend && mvn clean package -DskipTests`
3. Start command: `java -jar target/*.jar`
4. Set PostgreSQL add-on
5. Add env vars from above

**Frontend:**
1. Create new Static Site
2. Build command: `cd frontend && npm install && npm run build`
3. Publish directory: `frontend/dist`
4. Add `VITE_API_BASE_URL` pointing to backend

### Option 2: Railway.app (Free Tier)

1. Create new project
2. Add PostgreSQL plugin
3. Deploy backend from `backend/` directory
4. Deploy frontend from `frontend/` directory
5. Set environment variables in Railway dashboard

### Option 3: Heroku (Free Tier - verify pricing)

```bash
heroku create reconcileiq-backend
heroku addons:create heroku-postgresql:hobby-dev
git push heroku main
```

Add environment variables via Heroku dashboard.

## Verification Checklist

- [ ] Database containers healthy
- [ ] Backend responds at `/actuator/health` (returns `{"status":"UP"}`)
- [ ] Frontend loads and shows login page
- [ ] Login with exec@demo.com succeeds
- [ ] Login with founder@demo.com succeeds
- [ ] Wrong password returns 401 with generic message
- [ ] Dashboard loads after login
- [ ] Orders page shows data
- [ ] Claims flow works (create, submit, approve)

## Troubleshooting

### Login fails with 401

1. Verify password matches `DEMO_EXEC_PASSWORD` or `DEMO_FOUNDER_PASSWORD` env var
2. Check backend logs for authentication errors
3. Ensure JWT_SECRET hasn't been reset (invalidates all tokens)

### CORS errors

1. Verify `CORS_ALLOWED_ORIGINS` includes your frontend URL
2. Check browser console for specific origin mismatch

### Database connection refused

1. `docker compose up postgres` to start DB
2. Verify credentials match env vars
3. Run `docker compose logs postgres` for errors

### Frontend blank page after login

1. Check browser localStorage for `reconcileiq_token`
2. Verify token is not expired
3. Check Network tab for 401/403 responses

## Local Development

### Backend

```bash
cd backend
mvn spring-boot:run
# Access at http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Access at http://localhost:5173
```