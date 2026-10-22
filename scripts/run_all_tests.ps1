# WeatherEngine Full Test Runner for Windows PowerShell

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Executing Spring Boot / Kotlin Automated Tests (Gradle)" -ForegroundColor Cyan
Write-Host "=========================================================="

.\gradlew.bat test --no-daemon

if ($LASTEXITCODE -ne 0) {
    Write-Host "Gradle test execution failed with exit code $LASTEXITCODE" -ForegroundColor Red
} else {
    Write-Host "All Gradle unit tests passed successfully!" -ForegroundColor Green
}

Write-Host "Checking if local backend server is running on localhost:8080..."
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -TimeoutSec 2
    if ($health.status -eq "UP") {
        Write-Host "Local backend is UP. Running API Integration Suite..." -ForegroundColor Green
        .\scripts\test_api.ps1 -BaseUrl "http://localhost:8080"
    }
} catch {
    Write-Host "[NOTE] Local backend not detected on http://localhost:8080. Start via 'docker compose up -d' or '.\gradlew.bat bootRun' to run full live API tests." -ForegroundColor Yellow
}
