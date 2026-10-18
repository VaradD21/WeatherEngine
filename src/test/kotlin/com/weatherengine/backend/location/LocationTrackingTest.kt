package com.weatherengine.backend.location

import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.weather.OpenMeteoCurrentWeather
import com.weatherengine.backend.weather.OpenMeteoForecastResponse
import com.weatherengine.backend.weather.OpenMeteoHourlyForecast
import com.weatherengine.backend.weather.WeatherBundleDto
import com.weatherengine.backend.weather.WeatherCacheService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class LocationTrackingTest {

    private val repository = mockk<UserLocationHistoryRepository>()
    private val weatherCacheService = mockk<WeatherCacheService>()
    private val service = LocationTrackingService(repository, weatherCacheService)
    private val controller = UserLocationController(service)

    @Test
    fun `recordLocation saves new entry when no prior history exists`() {
        val userId = UUID.randomUUID()
        every { repository.findTopByUserIdOrderByRecordedAtDesc(userId) } returns null
        every { repository.save(any()) } answers { firstArg() }
        every { repository.findByUserIdAndDayOfWeekAndHourOfDay(any(), any(), any()) } returns emptyList()
        every { weatherCacheService.getCachedWeatherBundle(any(), any()) } returns createMockBundle()

        val request = RecordLocationRequest(latitude = 28.613912, longitude = 77.209045, accuracyMeters = 10.5f)
        val response = controller.recordLocation(AuthenticatedUser(userId, "user@test.com"), request)

        assertNotNull(response.body)
        assertTrue(response.body!!.isNewSlice)
        assertEquals(28.6139, response.body!!.latitude, 0.0001)
        assertEquals(77.2090, response.body!!.longitude, 0.0001)

        verify(exactly = 1) { repository.save(match { it.latitude == 28.6139 && it.longitude == 77.2090 }) }
    }

    @Test
    fun `recordLocation debounces and merges nearby snapshot within 45 minutes`() {
        val userId = UUID.randomUUID()
        val existing = UserLocationHistory(
            id = 101L,
            userId = userId,
            latitude = 28.6139,
            longitude = 77.2090,
            hourOfDay = 14,
            dayOfWeek = 1,
            recordedAt = Instant.now().minusSeconds(10 * 60) // 10 minutes ago
        )

        every { repository.findTopByUserIdOrderByRecordedAtDesc(userId) } returns existing
        every { repository.save(any()) } answers { firstArg() }
        every { repository.findByUserIdAndDayOfWeekAndHourOfDay(any(), any(), any()) } returns emptyList()
        every { weatherCacheService.getCachedWeatherBundle(any(), any()) } returns createMockBundle()

        // User is at the same location (moved only ~10 meters)
        val request = RecordLocationRequest(latitude = 28.61392, longitude = 77.20901, accuracyMeters = 8.0f)
        val response = controller.recordLocation(AuthenticatedUser(userId, "user@test.com"), request)

        assertNotNull(response.body)
        assertFalse(response.body!!.isNewSlice, "Should debounce and avoid inserting duplicate record")
    }

    @Test
    fun `predictive alert detects high rain probability for user routine destination`() {
        val userId = UUID.randomUUID()
        every { repository.findTopByUserIdOrderByRecordedAtDesc(userId) } returns null
        every { repository.save(any()) } answers { firstArg() }
        every { repository.findByUserIdAndDayOfWeekAndHourOfDay(any(), any(), any()) } returns emptyList()

        val mockBundleWithRain = createMockBundle(precipProbability = 85, precipMm = 6.0)
        every { weatherCacheService.getCachedWeatherBundle(any(), any()) } returns mockBundleWithRain

        val request = RecordLocationRequest(latitude = 28.6139, longitude = 77.2090)
        val response = controller.recordLocation(AuthenticatedUser(userId, "user@test.com"), request)

        assertNotNull(response.body)
        val alert = response.body?.predictedAlert
        assertNotNull(alert, "Should trigger a predictive rain alert")
        assertEquals("RAIN", alert?.alertType)
        assertEquals("CRITICAL", alert?.severity)
        assertTrue(alert!!.message.contains("umbrella", ignoreCase = true))
    }

    private fun createMockBundle(precipProbability: Int = 10, precipMm: Double = 0.0): WeatherBundleDto {
        val times = (0..23).map { String.format("2026-10-18T%02d:00", it) }
        val probs = times.map { precipProbability }
        val precips = times.map { precipMm }
        val temps = times.map { 26.0 }
        val codes = times.map { if (precipMm > 0) 63 else 0 }

        return WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                current = OpenMeteoCurrentWeather(temperature2m = 26.0),
                hourly = OpenMeteoHourlyForecast(
                    time = times,
                    temperature2m = temps,
                    precipitationProbability = probs,
                    precipitation = precips,
                    weatherCode = codes
                )
            ),
            airQuality = null,
            marine = null,
            fetchedAtMs = System.currentTimeMillis()
        )
    }
}
