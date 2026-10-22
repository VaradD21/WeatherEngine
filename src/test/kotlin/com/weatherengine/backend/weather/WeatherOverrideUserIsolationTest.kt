package com.weatherengine.backend.weather

import com.weatherengine.backend.testing.WeatherOverrideDto
import com.weatherengine.backend.testing.WeatherOverrideService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class WeatherOverrideUserIsolationTest {

    @Test
    fun `userA with active override sees overridden weather while userB at same coords sees normal weather`() {
        val userA = UUID.randomUUID()
        val userB = UUID.randomUUID()
        val lat = 28.61
        val lon = 77.20

        val normalBundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                latitude = lat,
                longitude = lon,
                current = OpenMeteoCurrentWeather(apparentTemperature = 25.0, temperature2m = 25.0, weatherCode = 0)
            )
        )

        val openMeteoClient = mockk<OpenMeteoClient>()
        every { openMeteoClient.fetchForecast(lat, lon) } returns normalBundle.forecast
        every { openMeteoClient.fetchAirQuality(lat, lon) } returns null
        every { openMeteoClient.fetchMarine(lat, lon) } returns null

        val weatherOverrideService = WeatherOverrideService()
        // Save override targeted specifically to userA
        weatherOverrideService.saveOverride(
            WeatherOverrideDto(
                id = "override_userA",
                targetUserId = userA,
                latitude = lat,
                longitude = lon,
                radiusKm = 20.0,
                temperature2m = 48.0,
                apparentTemperature = 52.0,
                weatherCode = 95,
                active = true
            )
        )

        val cacheService = WeatherCacheService(
            openMeteoClient = openMeteoClient,
            weatherOverrideService = weatherOverrideService
        )

        // UserA requests weather -> sees override (48.0°C, code 95)
        val bundleUserA = cacheService.getCachedWeatherBundle(lat, lon, userA)
        assertEquals(48.0, bundleUserA.forecast.current?.temperature2m)
        assertEquals(52.0, bundleUserA.forecast.current?.apparentTemperature)
        assertEquals(95, bundleUserA.forecast.current?.weatherCode)

        // UserB requests weather at exact same coordinates -> sees normal weather (25.0°C, code 0)
        val bundleUserB = cacheService.getCachedWeatherBundle(lat, lon, userB)
        assertEquals(25.0, bundleUserB.forecast.current?.temperature2m)
        assertEquals(25.0, bundleUserB.forecast.current?.apparentTemperature)
        assertEquals(0, bundleUserB.forecast.current?.weatherCode)
    }
}
