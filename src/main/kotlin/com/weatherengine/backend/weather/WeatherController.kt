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

@Service
class WeatherCacheService(
    private val openMeteoClient: OpenMeteoClient,
    private val weatherApiClient: WeatherApiClient? = null,
    private val weatherOverrideService: WeatherOverrideService? = null,
    private val cacheManager: CacheManager? = null
) {
    @Cacheable(
        value = [WEATHER_CACHE_NAME],
        key = "'v3:' + T(java.lang.String).format(T(java.util.Locale).US, '%.2f,%.2f', #lat, #lon)"
    )
    fun getCachedWeatherBundle(lat: Double, lon: Double): WeatherBundleDto {
        val override = weatherOverrideService?.findOverride(lat, lon)
        if (override != null) {
            val base = try {
                fetchRawBundle(lat, lon)
            } catch (ex: Exception) {
                weatherOverrideService.createSyntheticBaseBundle(lat, lon)
            }
            return weatherOverrideService.applyOverride(base, override)
        }

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
            if (mappedAqi != null) {
                airQuality = OpenMeteoAirQualityResponse(
                    current = OpenMeteoCurrentAirQuality(
                        usAqi = mappedAqi,
                        pm25 = secondary?.current?.airQuality?.pm2_5,
                        grassPollen = airQuality?.current?.grassPollen,
                        birchPollen = airQuality?.current?.birchPollen,
                        alderPollen = airQuality?.current?.alderPollen,
                        ragweedPollen = airQuality?.current?.ragweedPollen
                    )
                )
            }
        }

        return WeatherBundleDto(
            forecast = forecast,
            airQuality = airQuality,
            marine = marine,
            fetchedAtMs = System.currentTimeMillis()
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
