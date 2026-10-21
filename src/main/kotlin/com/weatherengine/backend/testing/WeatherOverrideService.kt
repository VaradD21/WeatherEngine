package com.weatherengine.backend.testing

import com.weatherengine.backend.weather.*
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

const val WEATHER_CACHE_NAME = "weather-bundle-v3"

@Service
class WeatherOverrideService(
    private val cacheManager: CacheManager? = null
) {
    private val logger = LoggerFactory.getLogger(WeatherOverrideService::class.java)
    private val overrides = ConcurrentHashMap<String, WeatherOverrideDto>()

    fun getAllOverrides(): List<WeatherOverrideDto> = overrides.values.toList()

    fun getOverride(id: String): WeatherOverrideDto? = overrides[id]

    fun saveOverride(dto: WeatherOverrideDto): WeatherOverrideDto {
        overrides[dto.id] = dto
        evictCacheForLocation(dto.latitude, dto.longitude)
        logger.info("Saved weather override [id={}, lat={}, lon={}, aqi={}]", dto.id, dto.latitude, dto.longitude, dto.usAqi)
        return dto
    }

    fun deleteOverride(id: String): Boolean {
        val removed = overrides.remove(id)
        if (removed != null) {
            evictCacheForLocation(removed.latitude, removed.longitude)
            logger.info("Removed weather override [id={}]", id)
            return true
        }
        return false
    }

    fun clearAllOverrides() {
        val locs = overrides.values.map { it.latitude to it.longitude }
        overrides.clear()
        locs.forEach { (lat, lon) -> evictCacheForLocation(lat, lon) }
        cacheManager?.getCache(WEATHER_CACHE_NAME)?.clear()
        logger.info("Cleared all weather overrides")
    }

    fun findOverride(lat: Double, lon: Double, userId: UUID? = null): WeatherOverrideDto? {
        val active = overrides.values.filter { it.active }
        if (active.isEmpty()) return null

        if (userId != null) {
            val userMatch = active.firstOrNull { it.targetUserId == userId && isWithinRadius(lat, lon, it.latitude, it.longitude, it.radiusKm) }
            if (userMatch != null) return userMatch
        }

        return active.firstOrNull { it.targetUserId == null && isWithinRadius(lat, lon, it.latitude, it.longitude, it.radiusKm) }
    }

    fun applyOverride(baseBundle: WeatherBundleDto, override: WeatherOverrideDto): WeatherBundleDto {
        val curAir = baseBundle.airQuality?.current ?: OpenMeteoCurrentAirQuality()
        val curWth = baseBundle.forecast.current ?: OpenMeteoCurrentWeather()
        val curHourly = baseBundle.forecast.hourly ?: OpenMeteoHourlyForecast()
        val curDaily = baseBundle.forecast.daily ?: OpenMeteoDailyForecast()

        val updatedAirQuality = OpenMeteoAirQualityResponse(
            current = OpenMeteoCurrentAirQuality(
                usAqi = override.usAqi ?: curAir.usAqi,
                pm25 = override.pm25 ?: curAir.pm25,
                grassPollen = override.grassPollen ?: curAir.grassPollen,
                birchPollen = override.birchPollen ?: curAir.birchPollen,
                alderPollen = override.alderPollen ?: curAir.alderPollen,
                ragweedPollen = override.ragweedPollen ?: curAir.ragweedPollen
            )
        )

        val updatedCurrent = OpenMeteoCurrentWeather(
            time = curWth.time ?: java.time.LocalDateTime.now().toString(),
            temperature2m = override.temperature2m ?: curWth.temperature2m ?: 22.0,
            relativeHumidity2m = override.relativeHumidity2m ?: curWth.relativeHumidity2m ?: 55,
            apparentTemperature = override.apparentTemperature ?: override.temperature2m ?: curWth.apparentTemperature ?: 22.0,
            isDay = override.isDay ?: curWth.isDay ?: 1,
            precipitation = override.precipitation ?: curWth.precipitation ?: 0.0,
            weatherCode = override.weatherCode ?: curWth.weatherCode ?: 0,
            windSpeed10m = override.windSpeed10m ?: curWth.windSpeed10m ?: 12.0,
            windGusts10m = override.windGusts10m ?: curWth.windGusts10m ?: 18.0
        )

        val updatedHourly = if (override.uvIndex != null || override.visibility != null || override.precipitationProbability != null || override.apparentTemperature != null || override.temperature2m != null || override.weatherCode != null) {
            val count = max(curHourly.time.size, 24)
            val times = if (curHourly.time.isNotEmpty()) curHourly.time else (0..23).map { String.format(Locale.US, "%02d:00", it) }
            val uvs = if (override.uvIndex != null) List(count) { override.uvIndex } else curHourly.uvIndex
            val vis = if (override.visibility != null) List(count) { override.visibility } else curHourly.visibility
            val probs = if (override.precipitationProbability != null) List(count) { override.precipitationProbability } else curHourly.precipitationProbability
            val appTemps = if (override.apparentTemperature != null) List(count) { override.apparentTemperature } else curHourly.apparentTemperature
            val temps = if (override.temperature2m != null) List(count) { override.temperature2m } else curHourly.temperature2m
            val codes = if (override.weatherCode != null) List(count) { override.weatherCode } else curHourly.weatherCode

            OpenMeteoHourlyForecast(
                time = times,
                temperature2m = temps,
                apparentTemperature = appTemps,
                relativeHumidity2m = curHourly.relativeHumidity2m,
                precipitationProbability = probs,
                precipitation = curHourly.precipitation,
                weatherCode = codes,
                windSpeed10m = curHourly.windSpeed10m,
                windGusts10m = curHourly.windGusts10m,
                visibility = vis,
                uvIndex = uvs,
                isDay = curHourly.isDay
            )
        } else curHourly

        val updatedDaily = if (override.temperature2m != null || override.uvIndex != null || override.precipitation != null || override.weatherCode != null) {
            val dDays = max(curDaily.time.size, 7)
            val dTimes = if (curDaily.time.isNotEmpty()) curDaily.time else (0..6).map { "2026-10-${21 + it}" }
            val maxTemps = if (override.temperature2m != null) List(dDays) { override.temperature2m + 2.0 } else curDaily.temperature2mMax
            val minTemps = if (override.temperature2m != null) List(dDays) { override.temperature2m - 4.0 } else curDaily.temperature2mMin
            val maxUvs = if (override.uvIndex != null) List(dDays) { override.uvIndex } else curDaily.uvIndexMax
            val sunrises = if (curDaily.sunrise.isNotEmpty()) curDaily.sunrise else List(dDays) { "2026-10-21T06:15:00" }
            val sunsets = if (curDaily.sunset.isNotEmpty()) curDaily.sunset else List(dDays) { "2026-10-21T18:30:00" }
            val codes = if (override.weatherCode != null) List(dDays) { override.weatherCode } else curDaily.weatherCode

            OpenMeteoDailyForecast(
                time = dTimes,
                weatherCode = codes,
                temperature2mMax = maxTemps,
                temperature2mMin = minTemps,
                precipitationProbabilityMax = curDaily.precipitationProbabilityMax,
                precipitationSum = curDaily.precipitationSum,
                windSpeed10mMax = curDaily.windSpeed10mMax,
                uvIndexMax = maxUvs,
                sunrise = sunrises,
                sunset = sunsets
            )
        } else curDaily

        val updatedMarine = if (override.waveHeight != null || override.seaSurfaceTemperature != null) {
            val count = 24
            OpenMeteoMarineResponse(
                hourly = OpenMeteoMarineHourly(
                    time = (0..23).map { String.format(Locale.US, "%02d:00", it) },
                    waveHeight = List(count) { override.waveHeight ?: 1.0 },
                    wavePeriod = List(count) { 8.0 },
                    seaSurfaceTemperature = List(count) { override.seaSurfaceTemperature ?: 24.0 }
                )
            )
        } else baseBundle.marine

        return WeatherBundleDto(
            forecast = baseBundle.forecast.copy(
                latitude = override.latitude,
                longitude = override.longitude,
                current = updatedCurrent,
                hourly = updatedHourly,
                daily = updatedDaily
            ),
            airQuality = updatedAirQuality,
            marine = updatedMarine,
            fetchedAtMs = System.currentTimeMillis()
        )
    }

    fun createSyntheticBaseBundle(lat: Double, lon: Double): WeatherBundleDto {
        return WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                latitude = lat,
                longitude = lon,
                timezone = "UTC",
                current = OpenMeteoCurrentWeather(
                    temperature2m = 25.0,
                    apparentTemperature = 25.0,
                    relativeHumidity2m = 50,
                    isDay = 1,
                    precipitation = 0.0,
                    weatherCode = 0,
                    windSpeed10m = 10.0,
                    windGusts10m = 15.0
                ),
                hourly = OpenMeteoHourlyForecast(
                    time = (0..23).map { String.format(Locale.US, "2026-10-21T%02d:00", it) },
                    temperature2m = List(24) { 25.0 },
                    apparentTemperature = List(24) { 25.0 },
                    relativeHumidity2m = List(24) { 50 },
                    precipitationProbability = List(24) { 10 },
                    precipitation = List(24) { 0.0 },
                    weatherCode = List(24) { 0 },
                    visibility = List(24) { 10000.0 },
                    uvIndex = List(24) { 5.0 }
                ),
                daily = OpenMeteoDailyForecast(
                    time = (0..6).map { "2026-10-${21 + it}" },
                    weatherCode = List(7) { 0 },
                    temperature2mMax = List(7) { 28.0 },
                    temperature2mMin = List(7) { 18.0 },
                    uvIndexMax = List(7) { 6.0 },
                    sunrise = List(7) { "2026-10-21T06:00:00" },
                    sunset = List(7) { "2026-10-21T18:00:00" }
                )
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = 45, pm25 = 12.0)
            )
        )
    }

    private fun evictCacheForLocation(lat: Double, lon: Double) {
        val key = "v3:" + String.format(Locale.US, "%.2f,%.2f", lat, lon)
        try {
            cacheManager?.getCache(WEATHER_CACHE_NAME)?.evict(key)
        } catch (ex: Exception) {
            logger.warn("Cache eviction failed for key {}: {}", key, ex.message)
        }
    }

    private fun isWithinRadius(lat1: Double, lon1: Double, lat2: Double, lon2: Double, radiusKm: Double): Boolean {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = 6371.0 * c
        return distanceKm <= max(radiusKm, 1.0)
    }
}
