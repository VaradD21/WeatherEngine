package com.weatherengine.backend.testing

import com.weatherengine.backend.alert.AlertLog
import com.weatherengine.backend.alert.AlertLogDto
import com.weatherengine.backend.alert.AlertLogRepository
import com.weatherengine.backend.alert.WeatherAlertService
import com.weatherengine.backend.alert.toDto
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.location.PredictiveWeatherAlertDto
import com.weatherengine.backend.persona.PersonaService
import com.weatherengine.backend.user.*
import com.weatherengine.backend.weather.HomepageResponse
import com.weatherengine.backend.weather.WeatherBundleDto
import com.weatherengine.backend.weather.WeatherCacheService
import com.weatherengine.backend.weather.WidgetBuilder
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/testing")
@Profile("dev", "test")
class TestingApiController(
    private val weatherOverrideService: WeatherOverrideService,
    private val weatherCacheService: WeatherCacheService,
    private val weatherAlertService: WeatherAlertService,
    private val alertLogRepository: AlertLogRepository,
    private val userRepository: UserRepository,
    private val userHealthProfileRepository: UserHealthProfileRepository,
    private val personaService: PersonaService,
    private val widgetBuilder: WidgetBuilder
) {

    @GetMapping("/overrides")
    fun getOverrides(): ResponseEntity<List<WeatherOverrideDto>> =
        ResponseEntity.ok(weatherOverrideService.getAllOverrides())

    @PostMapping("/overrides")
    fun saveOverride(@RequestBody dto: WeatherOverrideDto): ResponseEntity<WeatherOverrideDto> {
        validateCoordinates(dto.latitude, dto.longitude)
        return ResponseEntity.ok(weatherOverrideService.saveOverride(dto))
    }

    @DeleteMapping("/overrides/{id}")
    fun deleteOverride(@PathVariable("id") id: String): ResponseEntity<Map<String, Any>> {
        val deleted = weatherOverrideService.deleteOverride(id)
        return ResponseEntity.ok(mapOf("success" to deleted, "id" to id))
    }

    @PostMapping("/overrides/clear")
    fun clearAllOverrides(): ResponseEntity<Map<String, String>> {
        weatherOverrideService.clearAllOverrides()
        return ResponseEntity.ok(mapOf("message" to "All overrides cleared and weather cache flushed"))
    }

    @PostMapping("/overrides/preset")
    fun applyPreset(@RequestBody req: PresetApplyRequest): ResponseEntity<WeatherOverrideDto> {
        validateCoordinates(req.latitude, req.longitude)
        val override = WeatherOverridePresets.createPreset(
            presetKey = req.presetKey,
            lat = req.latitude,
            lon = req.longitude,
            radiusKm = req.radiusKm,
            targetUserId = req.targetUserId,
            label = req.label
        )
        return ResponseEntity.ok(weatherOverrideService.saveOverride(override))
    }

    @PostMapping("/notifications/evaluate")
    fun evaluateNotifications(@RequestBody req: NotificationEvaluateRequest): ResponseEntity<List<PredictiveWeatherAlertDto>> {
        validateCoordinates(req.latitude, req.longitude)
        val healthProfile = req.userId?.let { userHealthProfileRepository.findById(it).orElse(null) }
        val bundle = weatherCacheService.getCachedWeatherBundle(req.latitude, req.longitude)
        val alerts = weatherAlertService.evaluateAlerts(
            bundle = bundle,
            healthProfile = healthProfile,
            targetHour = LocalDateTime.now().hour,
            lat = req.latitude,
            lon = req.longitude
        )
        return ResponseEntity.ok(alerts)
    }

    @PostMapping("/notifications/trigger")
    fun triggerNotification(@RequestBody req: NotificationTriggerRequest): ResponseEntity<AlertLogDto> {
        validateCoordinates(req.latitude, req.longitude)
        val healthProfile = req.userId?.let { userHealthProfileRepository.findById(it).orElse(null) }
        val bundle = weatherCacheService.getCachedWeatherBundle(req.latitude, req.longitude)
        val evaluated = weatherAlertService.evaluateAlerts(
            bundle = bundle,
            healthProfile = healthProfile,
            targetHour = LocalDateTime.now().hour,
            lat = req.latitude,
            lon = req.longitude
        )

        val top = evaluated.firstOrNull()
        val alertType = req.alertType ?: top?.alertType ?: "SIMULATED_TEST_ALERT"
        val headline = req.customHeadline ?: top?.headline ?: "Test Weather Notification"
        val message = req.customMessage ?: top?.message ?: "Testing notification delivery for location (${req.latitude}, ${req.longitude})"
        val severity = top?.severity ?: req.severity

        val log = alertLogRepository.save(
            AlertLog(
                userId = req.userId,
                alertType = alertType,
                headline = headline,
                message = message,
                severity = severity,
                latitude = req.latitude,
                longitude = req.longitude
            )
        )
        return ResponseEntity.ok(log.toDto())
    }

    @GetMapping("/alert-logs")
    fun getAlertLogs(): ResponseEntity<List<AlertLogDto>> =
        ResponseEntity.ok(alertLogRepository.findTop50ByOrderBySentAtDesc().map { it.toDto() })

    @GetMapping("/users")
    fun getUsers(): ResponseEntity<List<UserSummaryDto>> {
        val users = userRepository.findAll()
        val summaries = users.map { u ->
            val health = u.id?.let { userHealthProfileRepository.findById(it).orElse(null) }
            val personas = u.id?.let { personaService.getUserPersonas(it).map { p -> p.displayName } } ?: emptyList()
            UserSummaryDto(
                id = u.id!!,
                email = u.email,
                username = u.username,
                personas = personas,
                aqiThreshold = health?.aqiThreshold ?: 100,
                uvThreshold = health?.uvThreshold ?: 6,
                alertsEnabled = health?.alertsEnabled ?: true
            )
        }
        return ResponseEntity.ok(summaries)
    }

    @GetMapping("/inspect-weather")
    fun inspectWeather(
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        @RequestParam(value = "userId", required = false) userId: UUID? = null
    ): ResponseEntity<Map<String, Any?>> {
        validateCoordinates(lat, lon)
        val matchedOverride = weatherOverrideService.findOverride(lat, lon, userId)
        val bundle = weatherCacheService.getCachedWeatherBundle(lat, lon)
        return ResponseEntity.ok(
            mapOf(
                "isOverridden" to (matchedOverride != null),
                "matchedOverride" to matchedOverride,
                "bundle" to bundle
            )
        )
    }

    @GetMapping("/inspect-homepage")
    fun inspectHomepage(
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        @RequestParam(value = "userId", required = false) userId: UUID? = null,
        @RequestParam(value = "schoolStart", defaultValue = "08:00") schoolStart: String,
        @RequestParam(value = "schoolEnd", defaultValue = "15:00") schoolEnd: String
    ): ResponseEntity<HomepageResponse> {
        validateCoordinates(lat, lon)
        val bundle = weatherCacheService.getCachedWeatherBundle(lat, lon)

        val widgetCodes = if (userId != null) {
            val userCodes = personaService.getUserWidgetCodes(userId)
            if (userCodes.isNotEmpty()) userCodes else defaultAllWidgetCodes()
        } else {
            defaultAllWidgetCodes()
        }

        val widgets = widgetCodes.map { code ->
            widgetBuilder.buildWidget(code, bundle, schoolStart, schoolEnd)
        }

        return ResponseEntity.ok(
            HomepageResponse(
                widgets = widgets,
                message = "Simulated Homepage Widgets Preview (lat=$lat, lon=$lon, count=${widgets.size})"
            )
        )
    }

    private fun defaultAllWidgetCodes(): List<String> = listOf(
        "aqi_card", "pollen_card", "uv_index_card", "humidity_card",
        "sunrise_sunset_card", "best_running_hours_card", "wind_speed_card", "heat_alert_card",
        "traffic_card", "visibility_card", "storm_fog_alert_card",
        "wave_height_card", "sea_temp_card", "tide_card",
        "school_commute_card", "rain_alert_card", "severe_weather_card",
        "soil_moisture_card", "rainfall_prediction_card", "frost_alert_card"
    )

    private fun validateCoordinates(lat: Double, lon: Double) {
        if (lat.isNaN() || lat < -90.0 || lat > 90.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90")
        }
        if (lon.isNaN() || lon < -180.0 || lon > 180.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180")
        }
    }
}
