#!/usr/bin/env bash
# ==============================================================================
# WeatherEngine 24h Cloud Uptime & Latency Monitor
# Pings /actuator/health every 60s and logs timestamp, status, and latency.
# ==============================================================================
set -euo pipefail

PROD_URL="${1:-${PROD_URL:-}}"
if [ -z "$PROD_URL" ]; then
    echo "Usage: $0 https://your-app.onrender.com [output.csv]"
    exit 1
fi
PROD_URL="${PROD_URL%/}"
LOG_FILE="${2:-uptime.csv}"

if [ ! -f "$LOG_FILE" ]; then
    echo "timestamp_utc,http_code,status,duration_ms" > "$LOG_FILE"
fi

echo "Monitoring $PROD_URL/actuator/health -> Logging to $LOG_FILE"
echo "Press [CTRL+C] to stop."

while true; do
    TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    START_MS=$(date +%s%N)
    HTTP_CODE=$(curl -s -o /tmp/health_resp.json -w "%{http_code}" "$PROD_URL/actuator/health" || echo "000")
    END_MS=$(date +%s%N)
    DURATION_MS=$(( (END_MS - START_MS) / 1000000 ))

    STATUS="DOWN"
    if [ "$HTTP_CODE" = "200" ] && grep -q '"status":"UP"' /tmp/health_resp.json 2>/dev/null; then
        STATUS="UP"
    fi

    echo "$TS,$HTTP_CODE,$STATUS,$DURATION_MS" >> "$LOG_FILE"
    echo "[$TS] Status: $STATUS | HTTP $HTTP_CODE | Latency: ${DURATION_MS}ms"

    sleep 60
done
