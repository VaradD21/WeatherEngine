#!/usr/bin/env bash
# ==============================================================================
# WeatherEngine API Automation Suite
# Usage: ./scripts/test_api.sh [BASE_URL]
# Default BASE_URL: http://localhost:8080
# ==============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
echo "Running WeatherEngine API Test Suite against: $BASE_URL"

USER_A_EMAIL="test_user_a_$(date +%s)@example.com"
USER_B_EMAIL="test_user_b_$(date +%s)@example.com"
PASSWORD="SecurePassword123!"

echo "=================================================="
echo "SECTION 1: AUTH & REGISTRATION TESTS"
echo "=================================================="

# 1.1 Register User A
echo -n "[1.1] Register User A... "
SIGNUP_A_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_A_EMAIL\",\"password\":\"$PASSWORD\"}")
STATUS_A=$(echo "$SIGNUP_A_RES" | tail -n1)
BODY_A=$(echo "$SIGNUP_A_RES" | head -n-1)

if [[ "$STATUS_A" == "201" || "$STATUS_A" == "200" ]]; then
  TOKEN_A=$(echo "$BODY_A" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
  echo "PASS (HTTP $STATUS_A, Token received)"
else
  echo "FAIL (HTTP $STATUS_A, Body: $BODY_A)"
fi

# 1.2 Duplicate Email
echo -n "[1.2] Register duplicate email... "
DUP_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_A_EMAIL\",\"password\":\"$PASSWORD\"}")
STATUS_DUP=$(echo "$DUP_RES" | tail -n1)
BODY_DUP=$(echo "$DUP_RES" | head -n-1)

if [[ "$STATUS_DUP" == "409" ]]; then
  echo "PASS (HTTP 409 Conflict, Body: $BODY_DUP)"
else
  echo "FAIL (Expected 409, got $STATUS_DUP: $BODY_DUP)"
fi

# 1.3 Invalid email / short password
echo -n "[1.3] Register invalid email... "
INV_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"not-an-email\",\"password\":\"short\"}")
STATUS_INV=$(echo "$INV_RES" | tail -n1)
if [[ "$STATUS_INV" == "400" ]]; then
  echo "PASS (HTTP 400 Bad Request)"
else
  echo "FAIL (Expected 400, got $STATUS_INV)"
fi

# 1.5 Login User A
echo -n "[1.5] Login User A with correct creds... "
LOGIN_A_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_A_EMAIL\",\"password\":\"$PASSWORD\"}")
STATUS_LOGIN_A=$(echo "$LOGIN_A_RES" | tail -n1)
if [[ "$STATUS_LOGIN_A" == "200" ]]; then
  echo "PASS (HTTP 200 OK)"
else
  echo "FAIL (HTTP $STATUS_LOGIN_A)"
fi

# 1.6 Login wrong password
echo -n "[1.6] Login wrong password... "
WRONG_PW_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_A_EMAIL\",\"password\":\"WrongPassword!\"}")
STATUS_WRONG_PW=$(echo "$WRONG_PW_RES" | tail -n1)
if [[ "$STATUS_WRONG_PW" == "401" ]]; then
  echo "PASS (HTTP 401 Unauthorized)"
else
  echo "FAIL (Expected 401, got $STATUS_WRONG_PW)"
fi

# 1.7 Login non-existent user
echo -n "[1.7] Login non-existent user... "
NO_USER_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"doesnotexist@example.com\",\"password\":\"WrongPassword!\"}")
STATUS_NO_USER=$(echo "$NO_USER_RES" | tail -n1)
if [[ "$STATUS_NO_USER" == "401" ]]; then
  echo "PASS (HTTP 401 Unauthorized, same as 1.6)"
else
  echo "FAIL (Expected 401, got $STATUS_NO_USER)"
fi

# 1.10 Missing Authorization header
echo -n "[1.10] Protected endpoint without Auth header... "
NO_AUTH_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/users/me/personas")
STATUS_NO_AUTH=$(echo "$NO_AUTH_RES" | tail -n1)
if [[ "$STATUS_NO_AUTH" == "401" ]]; then
  echo "PASS (HTTP 401 Unauthorized)"
else
  echo "FAIL (Expected 401, got $STATUS_NO_AUTH)"
fi

echo "=================================================="
echo "SECTION 2 & 3: PERSONAS & ISOLATION"
echo "=================================================="

# Register User B
curl -s -X POST "$BASE_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_B_EMAIL\",\"password\":\"$PASSWORD\"}" > /dev/null

LOGIN_B_RES=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$USER_B_EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN_B=$(echo "$LOGIN_B_RES" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Set User A personas to [health_conscious, commuter]
echo -n "[3.2] User A sets personas [health_conscious, commuter]... "
SET_P_A=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/users/me/personas" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d '{"personaCodes":["health_conscious","commuter"]}')
STATUS_SET_P_A=$(echo "$SET_P_A" | tail -n1)
if [[ "$STATUS_SET_P_A" == "200" ]]; then
  echo "PASS (HTTP 200)"
else
  echo "FAIL (HTTP $STATUS_SET_P_A)"
fi

# Check User B personas (should be default health_conscious, NOT commuter)
echo -n "[2.1] Verify User B does not see User A personas... "
GET_P_B=$(curl -s -H "Authorization: Bearer $TOKEN_B" "$BASE_URL/api/users/me/personas")
if echo "$GET_P_B" | grep -q "commuter"; then
  echo "FAIL (Data leak: User B sees commuter!)"
else
  echo "PASS (Isolated: User B does not see User A's custom personas)"
fi

# 3.6 Select zero personas
echo -n "[3.6] User selects zero personas... "
ZERO_P_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/users/me/personas" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d '{"personaCodes":[]}')
STATUS_ZERO_P=$(echo "$ZERO_P_RES" | tail -n1)
if [[ "$STATUS_ZERO_P" == "400" ]]; then
  echo "PASS (HTTP 400 Bad Request)"
else
  echo "FAIL (Expected 400, got $STATUS_ZERO_P)"
fi

# 3.9 Invalid persona code
echo -n "[3.9] User submits invalid persona code... "
INV_P_RES=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/users/me/personas" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_A" \
  -d '{"personaCodes":["invalid_code_123"]}')
STATUS_INV_P=$(echo "$INV_P_RES" | tail -n1)
if [[ "$STATUS_INV_P" == "400" ]]; then
  echo "PASS (HTTP 400 Bad Request)"
else
  echo "FAIL (Expected 400, got $STATUS_INV_P)"
fi

echo "=================================================="
echo "SECTION 5: WEATHER API TESTS"
echo "=================================================="

# 5.1 Real weather fetch
echo -n "[5.1] Fetch weather for Nanded (19.15, 77.30)... "
WEATHER_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/weather?lat=19.15&lon=77.30")
STATUS_WEATHER=$(echo "$WEATHER_RES" | tail -n1)
BODY_WEATHER=$(echo "$WEATHER_RES" | head -n-1)
if [[ "$STATUS_WEATHER" == "200" ]] && echo "$BODY_WEATHER" | grep -q "forecast"; then
  echo "PASS (HTTP 200, forecast received)"
else
  echo "FAIL (HTTP $STATUS_WEATHER)"
fi

# 5.10 Coordinates out of bounds
echo -n "[5.10] Lat/Lon out of bounds (999.0, 77.0)... "
BOUNDS_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/weather?lat=999.0&lon=77.0")
STATUS_BOUNDS=$(echo "$BOUNDS_RES" | tail -n1)
if [[ "$STATUS_BOUNDS" == "400" ]]; then
  echo "PASS (HTTP 400 Bad Request)"
else
  echo "FAIL (Expected 400, got $STATUS_BOUNDS)"
fi

# 6.6 Health Check
echo -n "[6.6] Actuator health probe... "
HEALTH_RES=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
STATUS_HEALTH=$(echo "$HEALTH_RES" | tail -n1)
BODY_HEALTH=$(echo "$HEALTH_RES" | head -n-1)
if [[ "$STATUS_HEALTH" == "200" ]] && echo "$BODY_HEALTH" | grep -q "UP"; then
  echo "PASS (HTTP 200, status UP)"
else
  echo "FAIL (HTTP $STATUS_HEALTH, Body: $BODY_HEALTH)"
fi

echo "=================================================="
echo "API TEST RUN COMPLETE"
echo "=================================================="
