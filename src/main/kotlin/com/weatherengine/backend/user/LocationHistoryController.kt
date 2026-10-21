package com.weatherengine.backend.user

import com.weatherengine.backend.alert.AlertLog
import com.weatherengine.backend.alert.AlertLogRepository
import com.weatherengine.backend.alert.WeatherAlertService
import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.weather.WeatherCacheService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/users/me/location-history")
class LocationHistoryController(
    private val locationHistoryRepository: UserLocationHistoryRepository,
    private val userHealthProfileRepository: UserHealthProfileRepository,
    private val weatherCacheService: WeatherCacheService,
    private val weatherAlertService: WeatherAlertService,
    private val alertLogRepository: AlertLogRepository
) {

    @PostMapping
    @Transactional
    fun recordLocation(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestBody request: RecordLocationRequest
    ): ResponseEntity<LocationRecordResponse> {
        if (request.latitude < -90.0 || request.latitude > 90.0 || request.longitude < -180.0 || request.longitude > 180.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Invalid coordinates")
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val historyRecord = UserLocationHistory(
            userId = principal.userId,
            latitude = request.latitude,
            longitude = request.longitude,
            accuracyMeters = request.accuracyMeters,
            hourOfDay = now.hour,
            dayOfWeek = now.dayOfWeek.value,
            recordedAt = Instant.now()
        )
        locationHistoryRepository.save(historyRecord)

        val healthProfile = userHealthProfileRepository.findById(principal.userId).orElse(null)
        val bundle = weatherCacheService.getCachedWeatherBundle(request.latitude, request.longitude)
        val alerts = weatherAlertService.evaluateAlerts(
            bundle = bundle,
            healthProfile = healthProfile,
            targetHour = now.hour,
            lat = request.latitude,
            lon = request.longitude
        )

        val topAlert = alerts.firstOrNull()
        if (topAlert != null) {
            alertLogRepository.save(
                AlertLog(
                    userId = principal.userId,
                    alertType = topAlert.alertType,
                    headline = topAlert.headline,
                    message = topAlert.message,
                    severity = topAlert.severity,
                    latitude = request.latitude,
                    longitude = request.longitude
                )
            )
        }

        return ResponseEntity.ok(
            LocationRecordResponse(
                status = "OK",
                isNewSlice = true,
                latitude = request.latitude,
                longitude = request.longitude,
                recordedAt = Instant.now().toString(),
                predictedAlert = topAlert
            )
        )
    }

    @GetMapping("/predictive-alerts")
    @Transactional(readOnly = true)
    fun getPredictiveAlerts(
        @AuthenticationPrincipal principal: AuthenticatedUser
    ): ResponseEntity<List<PredictiveWeatherAlertDto>> {
        val lastLocation = locationHistoryRepository.findTop1ByUserIdOrderByRecordedAtDesc(principal.userId)
            ?: return ResponseEntity.ok(emptyList())

        val healthProfile = userHealthProfileRepository.findById(principal.userId).orElse(null)
        val bundle = weatherCacheService.getCachedWeatherBundle(lastLocation.latitude, lastLocation.longitude)
        val alerts = weatherAlertService.evaluateAlerts(
            bundle = bundle,
            healthProfile = healthProfile,
            targetHour = LocalDateTime.now().hour,
            lat = lastLocation.latitude,
            lon = lastLocation.longitude
        )
        return ResponseEntity.ok(alerts)
    }
}
