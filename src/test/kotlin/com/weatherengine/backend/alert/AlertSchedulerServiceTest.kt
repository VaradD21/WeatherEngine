package com.weatherengine.backend.alert

import com.weatherengine.backend.location.Location
import com.weatherengine.backend.user.User
import com.weatherengine.backend.user.UserHealthProfileRepository
import com.weatherengine.backend.weather.OpenMeteoCurrentWeather
import com.weatherengine.backend.weather.OpenMeteoForecastResponse
import com.weatherengine.backend.weather.WeatherBundleDto
import com.weatherengine.backend.weather.WeatherCacheService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class AlertSchedulerServiceTest {

    private val subscriptionRepository = mockk<AlertSubscriptionRepository>()
    private val alertLogRepository = mockk<AlertLogRepository>(relaxed = true)
    private val weatherCacheService = mockk<WeatherCacheService>()
    private val userHealthProfileRepository = mockk<UserHealthProfileRepository>()

    private val alertSchedulerService = AlertSchedulerService(
        subscriptionRepository,
        alertLogRepository,
        weatherCacheService,
        userHealthProfileRepository
    )

    @Test
    fun `does not send alert when duplicate alert logged within dedup window`() {
        val locationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "test@example.com", passwordHash = "hash")
        val location = Location(
            id = locationId,
            user = user,
            label = "Test City",
            latitude = BigDecimal("28.61"),
            longitude = BigDecimal("77.20")
        )
        val subscription = AlertSubscription(
            id = UUID.randomUUID(),
            user = user,
            location = location,
            fcmToken = "test-fcm-token",
            active = true
        )

        every { subscriptionRepository.findByActiveTrue() } returns listOf(subscription)
        every { userHealthProfileRepository.findById(userId) } returns Optional.empty()

        val mockBundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                latitude = 28.61,
                longitude = 77.20,
                current = OpenMeteoCurrentWeather(
                    apparentTemperature = 41.0,
                    weatherCode = 0
                )
            )
        )
        every { weatherCacheService.getCachedWeatherBundle(28.61, 77.20) } returns mockBundle

        // Simulate that an EXTREME_HEAT alert was already sent within dedup window
        every {
            alertLogRepository.findByLocationIdAndAlertTypeAndSentAtAfter(
                locationId = locationId,
                alertType = "EXTREME_HEAT",
                after = any()
            )
        } returns listOf(
            AlertLog(
                id = UUID.randomUUID(),
                location = location,
                alertType = "EXTREME_HEAT",
                sentAt = Instant.now()
            )
        )

        alertSchedulerService.evaluateAndSendAlerts()

        verify(exactly = 0) { alertLogRepository.save(any()) }
    }
}
