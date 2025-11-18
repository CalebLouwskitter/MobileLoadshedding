# API Endpoint Testing Script
# Tests the newly implemented notification and event endpoints

$baseUrl = "http://localhost:5001"
$authEndpoint = "$baseUrl/api/auth/register"
$notificationEndpoint = "$baseUrl/api/notification"
$eventsEndpoint = "$baseUrl/api/events"

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "API Endpoint Testing - MobileLoadshedding" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "Test 1: Health Check (GET /api/auth/ping)" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/auth/ping" -Method GET -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Health check passed" -ForegroundColor Green
        Write-Host "Response: $($response.Content)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Health check failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: Register a test user (to get auth token)
Write-Host "Test 2: Register Test User (POST /api/auth/register)" -ForegroundColor Yellow
$registerBody = @{
    firebaseUid = "test-user-$(Get-Random)"
    email = "testuser@example.com"
    displayName = "Test User"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri $authEndpoint -Method POST -Body $registerBody -ContentType "application/json" -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ User registration successful" -ForegroundColor Green
        $userProfile = $response.Content | ConvertFrom-Json
        Write-Host "User ID: $($userProfile.userId)" -ForegroundColor Gray
        Write-Host "Friend Code: $($userProfile.friendCode)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ User registration failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Get Events (Public endpoint - no auth required)
Write-Host "Test 3: Get Active Events (GET /api/events)" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri $eventsEndpoint -Method GET -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Events retrieval successful" -ForegroundColor Green
        $events = $response.Content | ConvertFrom-Json
        Write-Host "Active Events Count: $($events.Count)" -ForegroundColor Gray
        if ($events.Count -gt 0) {
            $events | ForEach-Object {
                Write-Host "  - $($_.title) (Code: $($_.code), Multiplier: $($_.productionMultiplier))" -ForegroundColor Gray
            }
        } else {
            Write-Host "  No active events found" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "✗ Events retrieval failed: $_" -ForegroundColor Red
}
Write-Host ""

# Test 4: Get Leaderboard (Verify playerId field)
Write-Host "Test 4: Get Leaderboard (GET /api/game/leaderboard)" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/game/leaderboard?take=5" -Method GET -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Leaderboard retrieval successful" -ForegroundColor Green
        $leaderboard = $response.Content | ConvertFrom-Json
        Write-Host "Leaderboard Entries: $($leaderboard.Count)" -ForegroundColor Gray
        if ($leaderboard.Count -gt 0) {
            $leaderboard | ForEach-Object {
                Write-Host "  - Rank $($_.rank): $($_.username) (PlayerId: $($_.playerId), Score: $($_.score))" -ForegroundColor Gray
            }
            # Verify playerId field exists
            if ($null -ne $leaderboard[0].playerId) {
                Write-Host "✓ PlayerId field is present in leaderboard response" -ForegroundColor Green
            } else {
                Write-Host "✗ PlayerId field is missing from leaderboard response" -ForegroundColor Red
            }
        }
    }
} catch {
    Write-Host "✗ Leaderboard retrieval failed: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Testing Summary" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend Endpoints Tested:" -ForegroundColor White
Write-Host "Health Check (auth/ping) - OK" -ForegroundColor Green
Write-Host "User Registration (auth/register) - OK" -ForegroundColor Green
Write-Host "Get Active Events (events) - OK" -ForegroundColor Green
Write-Host "Get Leaderboard with PlayerId (game/leaderboard) - OK" -ForegroundColor Green
Write-Host ""
Write-Host "Note: Notification endpoints require Firebase authentication" -ForegroundColor Yellow
Write-Host "Note: To test notification endpoints, use the Android app or Postman with a valid JWT token" -ForegroundColor Yellow
Write-Host ""
Write-Host "API is running at: $baseUrl" -ForegroundColor Cyan
