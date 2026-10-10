package com.weatherengine.backend.user

import com.weatherengine.backend.alert.AlertSchedulerService
import com.weatherengine.backend.alert.AlertSubscriptionRepository
import com.weatherengine.backend.alert.AlertLogRepository
import com.weatherengine.backend.auth.AuthenticatedUser
import com.weatherengine.backend.weather.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class UserHealthProfileTest {

    private val healthProfileRepository = mockk<UserHealthProfileRepository>()
    private val controller = UserHealthProfileController(healthProfileRepository)

    @Test
    fun `getProfile returns default values when no profile exists yet`() {
        val userId = UUID.randomUUID()
        every { healthProfileRepository.findById(userId) } returns Optional.empty()

        val response = controller.getProfile(AuthenticatedUser(userId, "test@example.com"))

        assertNotNull(response.body)
        assertEquals(100, response.body?.aqiThreshold)
        assertEquals(6, response.body?.uvThreshold)
        assertEquals(70, response.body?.humidityThreshold)
        assertFalse(response.body!!.hasAsthma)
    }

    @Test
    fun `updateProfile saves and returns updated health profile`() {
        val userId = UUID.randomUUID()
        val existing = UserHealthProfile(userId = userId)
        every { healthProfileRepository.findById(userId) } returns Optional.of(existing)
        every { healthProfileRepository.save(any()) } answers { firstArg() }

        val request = HealthProfileDto(
            hasAsthma = true,
            hasAllergies = true,
            hasSkinSensitivity = true,
            aqiThreshold = 80,
            uvThreshold = 5,
            humidityThreshold = 65,
            pollenThreshold = "HIGH",
            alertsEnabled = true
        )

        val response = controller.updateProfile(AuthenticatedUser(userId, "test@example.com"), request)

        assertNotNull(response.body)
        assertTrue(response.body!!.hasAsthma)
        assertEquals(80, response.body!!.aqiThreshold)
        assertEquals(5, response.body!!.uvThreshold)
        assertEquals(65, response.body!!.humidityThreshold)
    }

    @Test
    fun `alert scheduler triggers custom asthma AQI alert when threshold breached`() {
        val subscriptionRepo = mockk<AlertSubscriptionRepository>()
        val alertLogRepo = mockk<AlertLogRepository>()
        val weatherCacheService = mockk<WeatherCacheService>()

        val scheduler = AlertSchedulerService(
            subscriptionRepository = subscriptionRepo,
            alertLogRepository = alertLogRepo,
            weatherCacheService = weatherCacheService,
            userHealthProfileRepository = healthProfileRepository
        )

        val bundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                current = OpenMeteoCurrentWeather(weatherCode = 0, apparentTemperature = 25.0)
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = 85)
            )
        )

        val profile = UserHealthProfile(
            userId = UUID.randomUUID(),
            hasAsthma = true,
            aqiThreshold = 80,
            alertsEnabled = true
        )

        val alertType = scheduler.detectAlertCondition(bundle, profile)
        assertEquals("ASTHMA_AQI_ALERT", alertType)
    }

    @Test
    fun `alert scheduler triggers skin UV alert when UV threshold breached`() {
        val subscriptionRepo = mockk<AlertSubscriptionRepository>()
        val alertLogRepo = mockk<AlertLogRepository>()
        val weatherCacheService = mockk<WeatherCacheService>()

        val scheduler = AlertSchedulerService(
            subscriptionRepository = subscriptionRepo,
            alertLogRepository = alertLogRepo,
            weatherCacheService = weatherCacheService,
            userHealthProfileRepository = healthProfileRepository
        )

        val bundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                current = OpenMeteoCurrentWeather(weatherCode = 0, apparentTemperature = 25.0),
                daily = OpenMeteoDailyForecast(uvIndexMax = listOf(7.0))
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(usAqi = 40)
            )
        )

        val profile = UserHealthProfile(
            userId = UUID.randomUUID(),
            hasSkinSensitivity = true,
            uvThreshold = 6,
            alertsEnabled = true
        )

        val alertType = scheduler.detectAlertCondition(bundle, profile)
        assertEquals("SKIN_UV_ALERT", alertType)
    }
}
