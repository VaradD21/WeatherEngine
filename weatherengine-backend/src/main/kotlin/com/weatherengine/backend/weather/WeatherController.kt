package com.weatherengine.backend.weather

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class WeatherController(
    private val weatherCacheService: WeatherCacheService,
    private val personaService: PersonaService,
    private val widgetBuilder: WidgetBuilder,
    private val preferencesRepository: com.weatherengine.backend.user.UserPreferencesRepository? = null
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(WeatherController::class.java)

    companion object {
        private const val DEFAULT_SCHOOL_START = "08:00"
        private const val DEFAULT_SCHOOL_END = "15:00"
    }

    @GetMapping("/weather")
    fun getWeather(
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double
    ): ResponseEntity<WeatherBundleDto> {
        validateCoordinates(lat, lon)
        return ResponseEntity.ok(weatherCacheService.getCachedWeatherBundle(lat, lon))
    }

    @GetMapping("/homepage")
    fun getHomepage(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        @RequestParam(name = "schoolStart", defaultValue = "08:00") schoolStart: String,
        @RequestParam(name = "schoolEnd", defaultValue = "15:00") schoolEnd: String
    ): ResponseEntity<HomepageResponse> {
        validateCoordinates(lat, lon)

        // Validate incoming request parameters first (400 if client supplied invalid query params)
        try {
            ParentPersonaRules.parseAndValidateSchoolHours(schoolStart, schoolEnd)
        } catch (ex: IllegalArgumentException) {
            throw ApiException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid school hours")
        }

        val userPrefs = preferencesRepository?.findById(principal.userId)?.orElse(null)

        // Determine effective hours: if client used default params and user has custom stored hours,
        // use stored hours. If stored hours in DB are corrupted, fall back to defaults with warning.
        val (effectiveStart, effectiveEnd) = if (
            schoolStart == DEFAULT_SCHOOL_START &&
            schoolEnd == DEFAULT_SCHOOL_END &&
            userPrefs != null &&
            (!userPrefs.schoolStart.isNullOrBlank() || !userPrefs.schoolEnd.isNullOrBlank())
        ) {
            val s = userPrefs.schoolStart.ifBlank { DEFAULT_SCHOOL_START }
            val e = userPrefs.schoolEnd.ifBlank { DEFAULT_SCHOOL_END }
            try {
                ParentPersonaRules.parseAndValidateSchoolHours(s, e)
                s to e
            } catch (ex: IllegalArgumentException) {
                logger.warn(
                    "Stored school hours are invalid for user {} (start='{}', end='{}'): {} — falling back to defaults",
                    principal.userId, s, e, ex.message
                )
                DEFAULT_SCHOOL_START to DEFAULT_SCHOOL_END
            }
        } else {
            schoolStart to schoolEnd
        }

        val defaultCodes = personaService.getUserWidgetCodes(principal.userId)
        if (defaultCodes.isEmpty()) {
            return ResponseEntity.ok(
                HomepageResponse(
                    widgets = emptyList(),
                    message = "No personas selected yet",
                    preferences = userPrefs?.toDto()
                )
            )
        }

        val disabledSet = userPrefs?.disabledWidgets?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        val customOrder = userPrefs?.widgetOrder?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

        val finalCodes = if (customOrder.isNotEmpty()) {
            val ordered = customOrder.filter { it in defaultCodes && it !in disabledSet }
            val remaining = defaultCodes.filter { it !in ordered && it !in disabledSet }
            ordered + remaining
        } else {
            defaultCodes.filter { it !in disabledSet }
        }

        val bundle = weatherCacheService.getCachedWeatherBundle(lat, lon)
        val widgets = finalCodes.map { code -> widgetBuilder.buildWidget(code, bundle, effectiveStart, effectiveEnd) }
        return ResponseEntity.ok(HomepageResponse(widgets = widgets, preferences = userPrefs?.toDto()))
    }

    private fun com.weatherengine.backend.user.UserPreferences.toDto(): com.weatherengine.backend.user.UserPreferencesDto {
        val order = this.widgetOrder?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        val disabled = this.disabledWidgets?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        return com.weatherengine.backend.user.UserPreferencesDto(
            widgetOrder = order,
            disabledWidgets = disabled,
            defaultScreen = this.defaultScreen,
            temperatureUnit = this.temperatureUnit,
            schoolStart = this.schoolStart,
            schoolEnd = this.schoolEnd,
            updatedAt = this.updatedAt
        )
    }

    private fun validateCoordinates(lat: Double, lon: Double) {
        if (lat.isNaN() || lat.isInfinite() || lat < -90.0 || lat > 90.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Latitude must be between -90 and 90")
        }
        if (lon.isNaN() || lon.isInfinite() || lon < -180.0 || lon > 180.0) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Longitude must be between -180 and 180")
        }
    }
}

