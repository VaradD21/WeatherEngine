package com.weatherengine.backend.location

import com.weatherengine.backend.auth.AuthenticatedUser
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users/me/location-history")
class UserLocationController(
    private val locationTrackingService: LocationTrackingService
) {

    @PostMapping
    fun recordLocation(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @Valid @RequestBody request: RecordLocationRequest
    ): ResponseEntity<LocationRecordResponse> {
        val response = locationTrackingService.recordLocation(user.userId, request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/recent")
    fun getRecentHistory(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @RequestParam(defaultValue = "24") limit: Int
    ): ResponseEntity<List<LocationHistoryItemDto>> {
        val history = locationTrackingService.getRecentHistory(user.userId, limit)
        return ResponseEntity.ok(history)
    }

    @GetMapping("/predictive-alerts")
    fun getPredictiveAlerts(
        @AuthenticationPrincipal user: AuthenticatedUser
    ): ResponseEntity<List<PredictiveWeatherAlertDto>> {
        val alerts = locationTrackingService.getUpcomingAlerts(user.userId)
        return ResponseEntity.ok(alerts)
    }
}
