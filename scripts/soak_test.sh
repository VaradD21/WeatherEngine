#!/usr/bin/env bash
# ==============================================================================
# WeatherEngine Soak Test Script
# Runs 10 minutes of steady moderate traffic (50 RPS) to detect memory/connection leaks
# ==============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
DURATION_MINUTES="${2:-10}"
END_TIME=$((SECONDS + DURATION_MINUTES * 60))

echo "Starting $DURATION_MINUTES-minute soak test against $BASE_URL..."

# Register soak user
EMAIL="soak_$(date +%s)@example.com"
PASSWORD="SoakPassword123!"

AUTH_RES=$(curl -s -X POST "$BASE_URL/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(echo "$AUTH_RES" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

REQUEST_COUNT=0
ERROR_COUNT=0

while [ $SECONDS -lt $END_TIME ]; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/api/homepage?lat=19.15&lon=77.30")

  REQUEST_COUNT=$((REQUEST_COUNT + 1))
  if [ "$STATUS" != "200" ]; then
    ERROR_COUNT=$((ERROR_COUNT + 1))
  fi

  if [ $((REQUEST_COUNT % 100)) -eq 0 ]; then
    echo "Progress: $REQUEST_COUNT requests sent. Errors: $ERROR_COUNT"
  fi

  sleep 0.1
done

echo "=========================================="
echo "Soak Test Completed."
echo "Total Requests: $REQUEST_COUNT"
echo "Total Errors: $ERROR_COUNT"
ERROR_RATE=$(awk "BEGIN {print ($ERROR_COUNT/$REQUEST_COUNT)*100}")
echo "Error Rate: $ERROR_RATE%"
echo "=========================================="
