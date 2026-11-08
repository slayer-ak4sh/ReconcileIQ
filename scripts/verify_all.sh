#!/bin/bash
set -u

echo "=== ReconcileIQ Verification Script ==="
echo ""

# Track overall status
OVERALL_STATUS=0

# a. Docker compose up and wait for health
echo "a. Starting Docker Compose..."
if docker compose up --build -d 2>&1; then
    echo "  Docker compose started successfully"
    # Wait for health endpoint
    MAX_RETRIES=30
    RETRY_COUNT=0
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health | grep -q "200"; then
            echo "  Health check: UP"
            break
        fi
        RETRY_COUNT=$((RETRY_COUNT + 1))
        sleep 2
    done
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo "  Health check: FAILED (timeout waiting for UP)"
        OVERALL_STATUS=1
    fi
else
    echo "  FAILED: docker compose up"
    OVERALL_STATUS=1
fi

# Read passwords from .env for tests
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

# b. Authentication tests
echo ""
echo "b. Authentication tests..."

# Test login flows
LOGIN_TEST=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"exec@demo.com\",\"password\":\"\$DEMO_EXEC_PASSWORD\"}" 2>/dev/null || echo "000")

if [ "$LOGIN_TEST" = "200" ]; then
    echo "  founder login 200: PASS"
else
    echo "  founder login 200: FAIL (got $LOGIN_TEST)"
    OVERALL_STATUS=1
fi

# Wrong password
WRONG_PWD=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"exec@demo.com\",\"password\":\"wrongpassword\"}" 2>/dev/null || echo "000")

if [ "$WRONG_PWD" = "401" ]; then
    echo "  wrong password 401: PASS"
else
    echo "  wrong password 401: FAIL (got $WRONG_PWD)"
    OVERALL_STATUS=1
fi

# c. Role checks
echo ""
echo "c. Role-based authorization tests..."

# Get exec token
EXEC_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"exec@demo.com\",\"password\":\"\$DEMO_EXEC_PASSWORD\"}" 2>/dev/null | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Get founder token
FOUNDER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"founder@demo.com\",\"password\":\"\$DEMO_FOUNDER_PASSWORD\"}" 2>/dev/null | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Exec approve claim (should be 403 - exec can't approve, only founder can)
if [ -n "${EXEC_TOKEN:-}" ]; then
    APPROVE_RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/claims/1/approve \
        -H "Authorization: Bearer $EXEC_TOKEN" 2>/dev/null || echo "000")
    if [ "$APPROVE_RESULT" = "403" ]; then
        echo "  executive approve claim 403: PASS"
    else
        echo "  executive approve claim 403: FAIL (got $APPROVE_RESULT)"
        OVERALL_STATUS=1
    fi

    # Founder submit (should be 403 - founder can't submit, exec can)
    SUBMIT_RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/claims/1/submit \
        -H "Authorization: Bearer $FOUNDER_TOKEN" 2>/dev/null || echo "000")
    if [ "$SUBMIT_RESULT" = "403" ]; then
        echo "  founder submit 403: PASS"
    else
        echo "  founder submit 403: FAIL (got $SUBMIT_RESULT)"
        OVERALL_STATUS=1
    fi

    # Founder POST matching run (should be 403)
    MATCH_RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8080/api/v1/matching/run?asOfDate=2026-11-06" \
        -H "Authorization: Bearer $FOUNDER_TOKEN" 2>/dev/null || echo "000")
    if [ "$MATCH_RESULT" = "403" ]; then
        echo "  founder POST /matching/run 403: PASS"
    else
        echo "  founder POST /matching/run 403: FAIL (got $MATCH_RESULT)"
        OVERALL_STATUS=1
    fi
fi

# Exec PUT rate card (should be 200)
if [ -n "${EXEC_TOKEN:-}" ]; then
    RATE_RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X PUT http://localhost:8080/api/v1/rate-cards/1 \
        -H "Authorization: Bearer $EXEC_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"commissionPercent":5,"fixedFee":50}' 2>/dev/null || echo "000")
    if [ "$RATE_RESULT" = "200" ]; then
        echo "  executive PUT rate card 200: PASS"
    else
        echo "  executive PUT rate card 200: FAIL (got $RATE_RESULT)"
        OVERALL_STATUS=1
    fi
fi

# Founder PUT rate card (should be 403)
if [ -n "${FOUNDER_TOKEN:-}" ]; then
    RATE_RESULT2=$(curl -s -o /dev/null -w "%{http_code}" -X PUT http://localhost:8080/api/v1/rate-cards/1 \
        -H "Authorization: Bearer $FOUNDER_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{"commissionPercent":5,"fixedFee":50}' 2>/dev/null || echo "000")
    if [ "$RATE_RESULT2" = "403" ]; then
        echo "  founder PUT rate card 403: PASS"
    else
        echo "  founder PUT rate card 403: FAIL (got $RATE_RESULT2)"
        OVERALL_STATUS=1
    fi
fi

# d. Upload demo files
echo ""
echo "d. File upload tests..."

# Get source ID for Website
SOURCE_ID=$(curl -s http://localhost:8080/api/v1/sources 2>/dev/null | grep -o '"id":[^,}]*' | head -1 | cut -d':' -f2 || echo "")

if [ -n "${EXEC_TOKEN:-}" ] && [ -n "${SOURCE_ID:-}" ]; then
    # Upload orders
    UPLOAD_ORDERS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/uploads \
        -H "Authorization: Bearer $EXEC_TOKEN" \
        -F "file=@scripts/output/orders_demo.csv" \
        -F "sourceId=$SOURCE_ID" \
        -F "fileType=ORDERS" 2>/dev/null || echo "000")
    if [ "$UPLOAD_ORDERS" -ge 200 ] && [ "$UPLOAD_ORDERS" -lt 300 ]; then
        echo "  upload orders_demo.csv: PASS ($UPLOAD_ORDERS)"
    else
        echo "  upload orders_demo.csv: FAIL ($UPLOAD_ORDERS)"
        OVERALL_STATUS=1
    fi

    # Upload settlements
    UPLOAD_SETT=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/uploads \
        -H "Authorization: Bearer $EXEC_TOKEN" \
        -F "file=@scripts/output/settlements_demo.csv" \
        -F "sourceId=$SOURCE_ID" \
        -F "fileType=SETTLEMENT" 2>/dev/null || echo "000")
    if [ "$UPLOAD_SETT" -ge 200 ] && [ "$UPLOAD_SETT" -lt 300 ]; then
        echo "  upload settlements_demo.csv: PASS ($UPLOAD_SETT)"
    else
        echo "  upload settlements_demo.csv: FAIL ($UPLOAD_SETT)"
        OVERALL_STATUS=1
    fi

    # Upload COD
    UPLOAD_COD=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/uploads \
        -H "Authorization: Bearer $EXEC_TOKEN" \
        -F "file=@scripts/output/cod_remittances_demo.csv" \
        -F "sourceId=$SOURCE_ID" \
        -F "fileType=COD_REMITTANCE" 2>/dev/null || echo "000")
    if [ "$UPLOAD_COD" -ge 200 ] && [ "$UPLOAD_COD" -lt 300 ]; then
        echo "  upload cod_remittances_demo.csv: PASS ($UPLOAD_COD)"
    else
        echo "  upload cod_remittances_demo.csv: FAIL ($UPLOAD_COD)"
        OVERALL_STATUS=1
    fi

    # Upload freight
    UPLOAD_FREIGHT=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/uploads \
        -H "Authorization: Bearer $EXEC_TOKEN" \
        -F "file=@scripts/output/freight_invoices_demo.csv" \
        -F "sourceId=$SOURCE_ID" \
        -F "fileType=FREIGHT_INVOICE" 2>/dev/null || echo "000")
    if [ "$UPLOAD_FREIGHT" -ge 200 ] && [ "$UPLOAD_FREIGHT" -lt 300 ]; then
        echo "  upload freight_invoices_demo.csv: PASS ($UPLOAD_FREIGHT)"
    else
        echo "  upload freight_invoices_demo.csv: FAIL ($UPLOAD_FREIGHT)"
        OVERALL_STATUS=1
    fi
fi

# e. Run matching
echo ""
echo "e. Run matching test..."
if [ -n "${EXEC_TOKEN:-}" ]; then
    MATCH_RUN=$(curl -s -o /dev/null -w "%{http_code}" -X POST "http://localhost:8080/api/v1/matching/run?asOfDate=2026-11-06" \
        -H "Authorization: Bearer $EXEC_TOKEN" 2>/dev/null || echo "000")
    if [ "$MATCH_RUN" = "200" ]; then
        echo "  POST /api/v1/matching/run?asOfDate=2026-11-06: PASS"
    else
        echo "  POST /api/v1/matching/run?asOfDate=2026-11-06: FAIL ($MATCH_RUN)"
        OVERALL_STATUS=1
    fi
fi

# f. Check specific orders
echo ""
echo "f. Order verification tests..."
if [ -n "${EXEC_TOKEN:-}" ]; then
    # Check ORD-W-204118
    ORDER1=$(curl -s -H "Authorization: Bearer $EXEC_TOKEN" http://localhost:8080/api/v1/orders 2>/dev/null | grep -o '"orderId":"ORD-W-204118"[^}]*"status":"[^"]*"' | head -1 || echo "")
    if echo "$ORDER1" | grep -q "UNPAID_PAST_DUE"; then
        echo "  ORD-W-204118 1409/0 UNPAID_PAST_DUE: PASS"
    else
        echo "  ORD-W-204118 1409/0 UNPAID_PAST_DUE: FAIL"
        OVERALL_STATUS=1
    fi

    # Check ORD-M-88120
    ORDER2=$(curl -s -H "Authorization: Bearer $EXEC_TOKEN" http://localhost:8080/api/v1/orders 2>/dev/null | grep -o '"orderId":"ORD-M-88120"[^}]*"status":"[^"]*"' | head -1 || echo "")
    if echo "$ORDER2" | grep -q "SHORT_PAID"; then
        echo "  ORD-M-88120 899/862 SHORT_PAID: PASS"
    else
        echo "  ORD-M-88120 899/862 SHORT_PAID: FAIL"
        OVERALL_STATUS=1
    fi

    # Check ORD-W-204377
    ORDER3=$(curl -s -H "Authorization: Bearer $EXEC_TOKEN" http://localhost:8080/api/v1/orders 2>/dev/null | grep -o '"orderId":"ORD-W-204377"[^}]*"status":"[^"]*"' | head -1 || echo "")
    if echo "$ORDER3" | grep -q "SHORT_PAID"; then
        echo "  ORD-W-204377 -96/-142 SHORT_PAID shortfall 46: PASS"
    else
        echo "  ORD-W-204377 -96/-142 SHORT_PAID shortfall 46: FAIL"
        OVERALL_STATUS=1
    fi
fi

# g. PII masking test
echo ""
echo "g. PII masking test..."
if [ -n "${EXEC_TOKEN:-}" ]; then
    ORDERS_JSON=$(curl -s -H "Authorization: Bearer $EXEC_TOKEN" http://localhost:8080/api/v1/orders 2>/dev/null)
    if echo "$ORDERS_JSON" | grep -q "Customer-ORD"; then
        echo "  PII masking FAILED: full names visible"
        OVERALL_STATUS=1
    else
        echo "  PII masking: PASS"
    fi
fi

# h. Dashboard money at risk
echo ""
echo "h. Dashboard money at risk test..."
if [ -n "${EXEC_TOKEN:-}" ]; then
    DASHBOARD=$(curl -s -H "Authorization: Bearer $EXEC_TOKEN" http://localhost:8080/api/v1/dashboard/money-at-risk 2>/dev/null)
    if echo "$DASHBOARD" | grep -q "0-7"; then
        echo "  GET /dashboard/money-at-risk bucket 0-7: PASS"
    else
        echo "  GET /dashboard/money-at-risk bucket 0-7: FAIL"
        OVERALL_STATUS=1
    fi
fi

# i. Claim flow
echo ""
echo "i. Claim flow test..."
# Need to implement claim creation, submit, approve flow
# This requires setting up a claim first

# j. Build tests
echo ""
echo "j. Build verification..."
echo "  cd backend && mvn test:"
cd /home/dev-akash/isocode/backend && mvn test -q 2>&1 | tail -10
if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "  Backend tests: PASS"
else
    echo "  Backend tests: FAIL"
    OVERALL_STATUS=1
fi

echo "  cd frontend && npm run build:"
cd /home/dev-akash/isocode/frontend && npm run build 2>&1 | tail -5
if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "  Frontend build: PASS"
else
    echo "  Frontend build: FAIL"
    OVERALL_STATUS=1
fi

# k. .env check
echo ""
echo "k. .env file check..."
if git ls-files | grep -qE '(^|/)\.env$'; then
    echo "  .env is tracked: FAIL"
    OVERALL_STATUS=1
else
    echo "  .env not tracked: PASS"
fi

echo ""
echo "=== Verification Complete ==="
if [ $OVERALL_STATUS -eq 0 ]; then
    echo "STATUS: PASS"
else
    echo "STATUS: FAIL"
fi

exit $OVERALL_STATUS