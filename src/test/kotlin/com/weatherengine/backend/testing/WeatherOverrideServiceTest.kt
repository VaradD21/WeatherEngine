package com.weatherengine.backend.testing

import com.weatherengine.backend.weather.OpenMeteoAirQualityResponse
import com.weatherengine.backend.weather.OpenMeteoCurrentAirQuality
import com.weatherengine.backend.weather.OpenMeteoForecastResponse
import com.weatherengine.backend.weather.WeatherBundleDto
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import java.util.UUID

class WeatherOverrideServiceTest {

    private val cacheManager = mockk<CacheManager>(relaxed = true)
    private lateinit var service: WeatherOverrideService

    @BeforeEach
    fun setUp() {
        service = WeatherOverrideService(cacheManager)
    }

    @Test
    fun `saveOverride stores override and findOverride matches within radius`() {
        val override = WeatherOverrideDto(
            latitude = 28.6139,
            longitude = 77.2090,
            radiusKm = 10.0,
            usAqi = 1000,
            pm25 = 480.0
        )
        service.saveOverride(override)

        val matched = service.findOverride(28.62, 77.21)
        assertNotNull(matched)
        assertEquals(1000, matched?.usAqi)

        val unmatched = service.findOverride(29.50, 77.20)
        assertNull(unmatched)
    }

    @Test
    fun `applyOverride updates AQI to 1000 and modifies current conditions`() {
        val baseBundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                latitude = 28.6139,
                longitude = 77.2090
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = 15, pm25 = 5.0)
            )
        )

        val override = WeatherOverrideDto(
            latitude = 28.6139,
            longitude = 77.2090,
            usAqi = 1000,
            pm25 = 450.0,
            temperature2m = 45.0,
            weatherCode = 95
        )

        val result = service.applyOverride(baseBundle, override)

        assertEquals(1000, result.airQuality?.current?.usAqi)
        assertEquals(450.0, result.airQuality?.current?.pm25)
        assertEquals(45.0, result.forecast.current?.temperature2m)
        assertEquals(95, result.forecast.current?.weatherCode)
    }

    @Test
    fun `user specific override takes precedence over generic location override`() {
        val targetUser = UUID.randomUUID()
        val otherUser = UUID.randomUUID()

        val globalOverride = WeatherOverrideDto(
            latitude = 28.6139,
            longitude = 77.2090,
            usAqi = 500,
            targetUserId = null
        )
        val userOverride = WeatherOverrideDto(
            latitude = 28.6139,
            longitude = 77.2090,
            usAqi = 1000,
            targetUserId = targetUser
        )

        service.saveOverride(globalOverride)
        service.saveOverride(userOverride)

        val matchForTarget = service.findOverride(28.6139, 77.2090, targetUser)
        assertEquals(1000, matchForTarget?.usAqi)

        val matchForOther = service.findOverride(28.6139, 77.2090, otherUser)
        assertEquals(500, matchForOther?.usAqi)
    }

    @Test
    fun `deleteOverride and clearAllOverrides evict properly`() {
        val o1 = WeatherOverrideDto(latitude = 19.07, longitude = 72.87, usAqi = 800)
        service.saveOverride(o1)
        assertEquals(1, service.getAllOverrides().size)

        service.deleteOverride(o1.id)
        assertTrue(service.getAllOverrides().isEmpty())
        assertNull(service.findOverride(19.07, 72.87))
    }
}
