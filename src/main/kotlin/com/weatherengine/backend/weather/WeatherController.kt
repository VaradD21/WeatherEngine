package com.weatherengine.backend.weather

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.common.ApiException
import com.weatherengine.backend.persona.PersonaService
import com.weatherengine.backend.testing.WEATHER_CACHE_NAME
import com.weatherengine.backend.testing.WeatherOverrideService
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class WeatherCacheService(
    private val openMeteoClient: OpenMeteoClient,
    private val weatherApiClient: WeatherApiClient? = null,
    private val weatherOverrideService: WeatherOverrideService? = null,
    private val cacheManager: CacheManager? = null
) {

    fun getCachedWeatherBundle(lat: Double, lon: Double, userId: UUID? = null): WeatherBundleDto {
        val overrideService = weatherOverrideService
        val override = overrideService?.findOverride(lat, lon, userId)
        if (override != null) {
            val base = try {
                fetchCachedBundle(lat, lon)
            } catch (ex: Exception) {
                overrideService.createSyntheticBaseBundle(lat, lon)
            }
            return overrideService.applyOverride(base, override)
        }

        return fetchCachedBundle(lat, lon)
    }

    @Cacheable(
        value = [WEATHER_CACHE_NAME],
        key = "'v3:' + T(java.lang.String).format(T(java.util.Locale).US, '%.2f,%.2f', #lat, #lon)"
    )
    fun fetchCachedBundle(lat: Double, lon: Double): WeatherBundleDto {
        return fetchRawBundle(lat, lon)
    }

    fun evictCache(lat: Double, lon: Double) {
        val key = "v3:" + String.format(Locale.US, "%.2f,%.2f", lat, lon)
        try {
            cacheManager?.getCache(WEATHER_CACHE_NAME)?.evict(key)
        } catch (_: Exception) {}
    }

    private fun fetchRawBundle(lat: Double, lon: Double): WeatherBundleDto {
        val forecast = openMeteoClient.fetchForecast(lat, lon)
        var airQuality = openMeteoClient.fetchAirQuality(lat, lon)
        val marine = openMeteoClient.fetchMarine(lat, lon)

        if ((airQuality?.current?.usAqi == null) && weatherApiClient?.isConfigured() == true) {
            val secondary = weatherApiClient.fetchCurrentAndAqi(lat, lon)
            val epaIndex = secondary?.current?.airQuality?.usEpaIndex
            val mappedAqi = when (epaIndex) {
                1 -> 25
                2 -> 75
                3 -> 125
                4 -> 175
                5 -> 250
                6 -> 350
                else -> null
            }
            if (mappedAqi != null && secondary?.current?.airQuality != null) {
                airQuality = OpenMeteoAirQualityResponse(
                    current = OpenMeteoCurrentAirQuality(
                        usAqi = mappedAqi,
                        pm25 = secondary.current.airQuality.pm2_5
                    )
                )
            }
        }

        return WeatherBundleDto(
            forecast = forecast,
            airQuality = airQuality,
            marine = marine
        )
    }
}

@RestController
@RequestMapping("/api")
class WeatherController(
    private val weatherCacheService: WeatherCacheService,
    private val personaService: PersonaService,
    private val widgetBuilder: WidgetBuilder,
    private val preferencesRepository: com.weatherengine.backend.user.UserPreferencesRepository? = null
) {

    companion object {
        private val lastForceRefresh = ConcurrentHashMap<String, Long>()
        private const val FORCE_REFRESH_COOLDOWN_MS = 60_000L

        fun resetRateLimitsForTesting() {
            lastForceRefresh.clear()
        }
    }

    private fun handleForceRefreshRateLimit(userId: UUID?) {
        val key = userId?.toString() ?: "anonymous"
        val now = System.currentTimeMillis()
        val last = lastForceRefresh[key]
        if (last != null && (now - last) < FORCE_REFRESH_COOLDOWN_MS) {
            val remaining = ((FORCE_REFRESH_COOLDOWN_MS - (now - last)) / 1000) + 1
            throw ApiException(HttpStatus.TOO_MANY_REQUESTS, "Force refresh rate limit exceeded. Please wait $remaining seconds.")
        }
        lastForceRefresh[key] = now
    }

    @GetMapping("/weather")
    fun getWeather(
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        @RequestParam(name = "forceRefresh", defaultValue = "false") forceRefresh: Boolean = false,
        @AuthenticationPrincipal principal: AuthenticatedUser? = null
    ): ResponseEntity<WeatherBundleDto> {
        validateCoordinates(lat, lon)
        if (forceRefresh) {
            handleForceRefreshRateLimit(principal?.userId)
            weatherCacheService.evictCache(lat, lon)
        }
        return ResponseEntity.ok(weatherCacheService.getCachedWeatherBundle(lat, lon, principal?.userId))
    }

    @GetMapping("/homepage")
    fun getHomepage(
        @AuthenticationPrincipal principal: AuthenticatedUser,
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        @RequestParam(name = "schoolStart", defaultValue = "08:00") schoolStart: String = "08:00",
        @RequestParam(name = "schoolEnd", defaultValue = "15:00") schoolEnd: String = "15:00",
        @RequestParam(name = "forceRefresh", defaultValue = "false") forceRefresh: Boolean = false
    ): ResponseEntity<HomepageResponse> {
        validateCoordinates(lat, lon)
        if (forceRefresh) {
            handleForceRefreshRateLimit(principal.userId)
            weatherCacheService.evictCache(lat, lon)
        }

        val userPrefs = preferencesRepository?.findById(principal.userId)?.orElse(null)
        val effectiveStart = if (schoolStart == "08:00" && !userPrefs?.schoolStart.isNullOrBlank()) userPrefs!!.schoolStart else schoolStart
        val effectiveEnd = if (schoolEnd == "15:00" && !userPrefs?.schoolEnd.isNullOrBlank()) userPrefs!!.schoolEnd else schoolEnd

        try {
            ParentPersonaRules.parseAndValidateSchoolHours(effectiveStart, effectiveEnd)
        } catch (ex: IllegalArgumentException) {
            throw ApiException(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid school hours")
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

        val bundle = weatherCacheService.getCachedWeatherBundle(lat, lon, principal.userId)
        val widgets = finalCodes.map { code ->
            widgetBuilder.buildWidget(code, bundle, effectiveStart, effectiveEnd, principal.userId)
        }
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
