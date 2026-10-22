# WeatherEngine API Automation Suite for Windows PowerShell
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$userA = "user_a_$timestamp@example.com"
$userB = "user_b_$timestamp@example.com"
$password = "SecurePassword123!"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "WEATHERENGINE API AUTOMATION SUITE" -ForegroundColor Cyan
Write-Host "Target Base URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "=================================================="

# 1.1 Register User A
Write-Host -NoNewline "[1.1] Register User A... "
try {
    $resA = Invoke-RestMethod -Uri "$BaseUrl/api/auth/signup" -Method Post -ContentType "application/json" -Body (@{ email = $userA; password = $password } | ConvertTo-Json)
    $tokenA = $resA.token
    Write-Host "PASS (Token received)" -ForegroundColor Green
} catch {
    Write-Host "FAIL ($($_))" -ForegroundColor Red
}

# 1.2 Duplicate registration
Write-Host -NoNewline "[1.2] Register duplicate email... "
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/auth/signup" -Method Post -ContentType "application/json" -Body (@{ email = $userA; password = $password } | ConvertTo-Json)
    Write-Host "FAIL (Expected 409 Conflict)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "PASS (HTTP 409 Conflict)" -ForegroundColor Green
    } else {
        Write-Host "FAIL (Status: $($_.Exception.Response.StatusCode))" -ForegroundColor Red
    }
}

# 1.5 Login User A
Write-Host -NoNewline "[1.5] Login User A... "
try {
    $loginA = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body (@{ email = $userA; password = $password } | ConvertTo-Json)
    Write-Host "PASS (HTTP 200 OK)" -ForegroundColor Green
} catch {
    Write-Host "FAIL ($($_))" -ForegroundColor Red
}

# 1.6 Login wrong password
Write-Host -NoNewline "[1.6] Login wrong password... "
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body (@{ email = $userA; password = "WrongPassword!" } | ConvertTo-Json)
    Write-Host "FAIL (Expected 401)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "PASS (HTTP 401 Unauthorized)" -ForegroundColor Green
    } else {
        Write-Host "FAIL" -ForegroundColor Red
    }
}

# 1.10 Missing Auth Header
Write-Host -NoNewline "[1.10] Missing Authorization header... "
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/users/me/personas" -Method Get
    Write-Host "FAIL (Expected 401)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "PASS (HTTP 401 Unauthorized)" -ForegroundColor Green
    } else {
        Write-Host "FAIL" -ForegroundColor Red
    }
}

# 5.1 Real weather fetch
Write-Host -NoNewline "[5.1] Fetch weather for Nanded (19.15, 77.30)... "
try {
    $weather = Invoke-RestMethod -Uri "$BaseUrl/api/weather?lat=19.15&lon=77.30" -Method Get
    if ($null -ne $weather.forecast) {
        Write-Host "PASS (HTTP 200, Forecast available)" -ForegroundColor Green
    } else {
        Write-Host "FAIL (Missing forecast)" -ForegroundColor Red
    }
} catch {
    Write-Host "FAIL ($($_))" -ForegroundColor Red
}

# 5.10 Out of bounds coordinate
Write-Host -NoNewline "[5.10] Coordinate out of bounds (999.0, 77.0)... "
try {
    Invoke-RestMethod -Uri "$BaseUrl/api/weather?lat=999.0&lon=77.0" -Method Get
    Write-Host "FAIL (Expected 400 Bad Request)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Write-Host "PASS (HTTP 400 Bad Request)" -ForegroundColor Green
    } else {
        Write-Host "FAIL" -ForegroundColor Red
    }
}

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "SUITE COMPLETED" -ForegroundColor Cyan
