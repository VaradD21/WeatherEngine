package com.weatherengine.backend.weather

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class WeatherCacheService(
    private val openMeteoClient: OpenMeteoClient
) {

    @Cacheable(value = ["weather-bundle"], key = "#latKey + ',' + #lonKey")
    fun getCachedWeatherBundle(
        latKey: String,
        lonKey: String,
        lat: Double,
        lon: Double
    ): WeatherBundleDto {
        val forecast = openMeteoClient.fetchForecast(lat, lon)
        val airQuality = openMeteoClient.fetchAirQuality(lat, lon)
        return WeatherBundleDto(
            forecast = forecast,
            airQuality = airQuality,
            fetchedAtMs = System.currentTimeMillis()
        )
    }
}
