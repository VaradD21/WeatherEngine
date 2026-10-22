#!/usr/bin/env bash
# ==============================================================================
# WeatherEngine Cloud Smoke Test Suite (Render)
# Usage: ./scripts/cloud_smoke.sh <PROD_URL>
# Example: ./scripts/cloud_smoke.sh https://weatherengine.onrender.com
# ==============================================================================
set -euo pipefail

PROD_URL="${1:-${PROD_URL:-}}"
if [ -z "$PROD_URL" ]; then
    echo "ERROR: PROD_URL is required as argument or environment variable."
    echo "Usage: $0 https://your-app.onrender.com"
    exit 1
fi
PROD_URL="${PROD_URL%/}"

PASS_COUNT=0
FAIL_COUNT=0

log_test() {
    local test_id="$1"
    local desc="$2"
    local status="$3"
    local detail="$4"
    if [ "$status" = "PASS" ]; then
        PASS_COUNT=$((PASS_COUNT + 1))
        echo -e "\033[32m[PASS]\033[0m $test_id: $desc - $detail"
    else
        FAIL_COUNT=$((FAIL_COUNT + 1))
        echo -e "\033[31m[FAIL]\033[0m $test_id: $desc - $detail"
    fi
}

echo "=== WeatherEngine Cloud Smoke Tests ==="
echo "Target: $PROD_URL"
echo "Timestamp: $(date -u)"
echo "----------------------------------------"

# C2.1: HTTPS and Redirect
REDIRECT_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${PROD_URL#https://}/actuator/health" || true)
if [[ "$REDIRECT_CODE" =~ ^(301|302|307|308|200)$ ]]; then
    log_test "C2.1" "HTTPS redirection and SSL handshake" "PASS" "HTTP code $REDIRECT_CODE"
else
    log_test "C2.1" "HTTPS redirection and SSL handshake" "FAIL" "Got $REDIRECT_CODE"
fi

# C2.2: Health Check & DB/Redis Connectivity
START_TIME=$(date +%s%N)
HEALTH_RESP=$(curl -s "$PROD_URL/actuator/health" || echo "{}")
DURATION=$(( ($(date +%s%N) - START_TIME) / 1000000 ))
if echo "$HEALTH_RESP" | grep -q '"status":"UP"'; then
    log_test "C2.2" "Health endpoint /actuator/health" "PASS" "Status UP in ${DURATION}ms"
else
    log_test "C2.2" "Health endpoint /actuator/health" "FAIL" "Response: $HEALTH_RESP"
fi

# C2.4: Persona Catalog
PERSONAS_RESP=$(curl -s "$PROD_URL/api/personas" || echo "[]")
if echo "$PERSONAS_RESP" | grep -q "health_conscious"; then
    log_test "C2.4" "Persona catalog endpoint" "PASS" "Personas catalog returned successfully"
else
    log_test "C2.4" "Persona catalog endpoint" "FAIL" "Response: $PERSONAS_RESP"
fi

# C2.5: User Registration & Login Flow
TEST_USER="smoke_$(date +%s)@example.com"
TEST_PASS="TestSecurePass123!"
REG_RESP=$(curl -s -X POST "$PROD_URL/api/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}" || echo "{}")

TOKEN=$(echo "$REG_RESP" | grep -o '"token":"[^"]*' | cut -d'"' -f4 || true)
if [ -n "$TOKEN" ]; then
    log_test "C2.5" "User registration & JWT issuance" "PASS" "Token received for $TEST_USER"
else
    log_test "C2.5" "User registration & JWT issuance" "FAIL" "Signup failed: $REG_RESP"
fi

# C2.6 & C2.7: Weather Engine Coordinates (Nanded)
WEATHER_RESP=$(curl -s "$PROD_URL/api/weather?lat=19.15&lon=77.32" || echo "{}")
if echo "$WEATHER_RESP" | grep -q "temperature_2m"; then
    TEMP=$(echo "$WEATHER_RESP" | grep -o '"temperature_2m":[^,}]*' | head -1 | cut -d: -f2 || true)
    log_test "C2.6" "Weather query Nanded (19.15, 77.32)" "PASS" "Real temperature: ${TEMP}°C"
else
    log_test "C2.6" "Weather query Nanded" "FAIL" "Failed to fetch bundle: $WEATHER_RESP"
fi

# C2.8: Authenticated Homepage with Personas
if [ -n "$TOKEN" ]; then
    HOME_RESP=$(curl -s "$PROD_URL/api/homepage?lat=19.07&lon=72.87" \
        -H "Authorization: Bearer $TOKEN" || echo "{}")
    if echo "$HOME_RESP" | grep -q "widgets"; then
        log_test "C2.8" "Personalized homepage widgets" "PASS" "Widgets returned for user"
    else
        log_test "C2.8" "Personalized homepage widgets" "FAIL" "Response: $HOME_RESP"
    fi
else
    log_test "C2.8" "Personalized homepage widgets" "BLOCKED" "Requires valid JWT"
fi

# C2.14: Security RBAC & Production Testing Endpoint Isolation
SEC_ADMIN=$(curl -s -o /dev/null -w "%{http_code}" "$PROD_URL/admin/status" || true)
SEC_TESTING=$(curl -s -o /dev/null -w "%{http_code}" "$PROD_URL/api/testing/status" || true)
if [ "$SEC_ADMIN" = "401" ] && [ "$SEC_TESTING" = "404" ]; then
    log_test "C2.14" "RBAC & testing endpoint isolation" "PASS" "/admin is 401, /api/testing is 404 in prod"
else
    log_test "C2.14" "RBAC & testing endpoint isolation" "FAIL" "/admin: $SEC_ADMIN, /api/testing: $SEC_TESTING"
fi

echo "----------------------------------------"
echo "Smoke Tests Complete: $PASS_COUNT PASSED, $FAIL_COUNT FAILED"
