package com.weatherengine.backend.weather

import com.weatherengine.backend.location.Location
import com.weatherengine.backend.user.User
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class TravelerRulesTest {

    @Test
    fun testFlightAlertCard_smoothConditions() {
        val current = OpenMeteoCurrentWeather(
            windSpeed10m = 10.0,
            windGusts10m = 15.0,
            weatherCode = 1
        )
        val hourly = OpenMeteoHourlyForecast(
            time = listOf("2026-10-12T10:00", "2026-10-12T11:00"),
            weatherCode = listOf(1, 2),
            windGusts10m = listOf(15.0, 18.0),
            visibility = listOf(10000.0, 10000.0),
            precipitationProbability = listOf(5, 10)
        )
        val now = LocalDateTime.parse("2026-10-12T10:00")
        val result = TravelerRules.computeFlightAlertCard(current, hourly, now)

        assertEquals("ok", result["status"])
        assertEquals("Minimal", result["delayRisk"])
        assertTrue((result["reason"] as String).contains("Calm skies"))
    }

    @Test
    fun testFlightAlertCard_severeThunderstorm() {
        val current = OpenMeteoCurrentWeather(
            windSpeed10m = 35.0,
            windGusts10m = 65.0,
            weatherCode = 95
        )
        val hourly = OpenMeteoHourlyForecast(
            time = listOf("2026-10-12T10:00"),
            weatherCode = listOf(95),
            windGusts10m = listOf(65.0),
            visibility = listOf(3000.0)
        )
        val now = LocalDateTime.parse("2026-10-12T10:00")
        val result = TravelerRules.computeFlightAlertCard(current, hourly, now)

        assertEquals("ok", result["status"])
        assertEquals("High", result["delayRisk"])
        assertTrue((result["reason"] as String).contains("Thunderstorms active"))
    }

    @Test
    fun testFlightAlertCard_denseFog() {
        val current = OpenMeteoCurrentWeather(weatherCode = 45)
        val hourly = OpenMeteoHourlyForecast(
            time = listOf("2026-10-12T06:00"),
            weatherCode = listOf(45),
            visibility = listOf(500.0)
        )
        val now = LocalDateTime.parse("2026-10-12T06:00")
        val result = TravelerRules.computeFlightAlertCard(current, hourly, now)

        assertEquals("Moderate", result["delayRisk"])
        assertTrue((result["reason"] as String).contains("Low visibility", ignoreCase = true))
    }

    @Test
    fun testPackingSuggestionCard_hotTropicalClimate() {
        val current = OpenMeteoCurrentWeather(temperature2m = 32.0)
        val daily = OpenMeteoDailyForecast(
            temperature2mMax = listOf(33.0, 34.0, 35.0),
            temperature2mMin = listOf(26.0, 27.0, 26.0),
            precipitationProbabilityMax = listOf(10, 5, 0),
            uvIndexMax = listOf(9.0, 10.0, 8.5)
        )
        val result = TravelerRules.computePackingSuggestionCard(current, daily, null)

        assertEquals("ok", result["status"])
        val items = result["items"] as List<*>
        assertTrue(items.any { (it as String).contains("cotton", ignoreCase = true) || it.contains("linen", ignoreCase = true) })
        assertTrue(items.any { (it as String).contains("sunscreen", ignoreCase = true) })
    }

    @Test
    fun testSavedDestinationsCard_zeroLocationsReturnsEmptyState() {
        val result = TravelerRules.computeSavedDestinationsCard(emptyList())

        assertEquals("empty", result["status"])
        val destinations = result["destinations"] as List<*>
        assertEquals(0, destinations.size)
        assertEquals("Add saved destinations in Locations tab", result["message"])
    }

    @Test
    fun testSavedDestinationsCard_twoLocationsReturnsThoseDestinations() {
        val mockUser = User(id = UUID.randomUUID(), email = "traveler@example.com", passwordHash = "hash")
        val loc1 = Location(
            id = UUID.randomUUID(),
            user = mockUser,
            label = "Mumbai HQ",
            latitude = BigDecimal("19.0760"),
            longitude = BigDecimal("72.8777")
        )
        val loc2 = Location(
            id = UUID.randomUUID(),
            user = mockUser,
            label = "Bengaluru Office",
            latitude = BigDecimal("12.9716"),
            longitude = BigDecimal("77.5946")
        )

        val result = TravelerRules.computeSavedDestinationsCard(listOf(loc1, loc2))

        assertEquals("ok", result["status"])
        val destinations = result["destinations"] as List<*>
        assertEquals(2, destinations.size)
        val d1 = destinations[0] as Map<*, *>
        val d2 = destinations[1] as Map<*, *>
        assertEquals("Mumbai HQ", d1["name"])
        assertEquals("Bengaluru Office", d2["name"])
    }
}
