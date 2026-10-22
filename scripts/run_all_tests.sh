#!/usr/bin/env bash
# ==============================================================================
# One-command runner for WeatherEngine Backend Tests
# ==============================================================================

set -euo pipefail

echo "=========================================================="
echo "Running All WeatherEngine Automated Unit & Mock Tests"
echo "=========================================================="

./gradlew test --no-daemon

echo "=========================================================="
echo "Unit and Service Tests Complete."
echo "Running API integration suite against running backend..."
echo "=========================================================="

if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
  ./scripts/test_api.sh "http://localhost:8080"
else
  echo "[NOTE] Backend is not running on localhost:8080. Start backend using docker-compose up to run API integration tests."
fi
