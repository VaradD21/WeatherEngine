package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
        assertEquals(false, result["hasDisruptionRisk"])
        assertEquals("Normal", result["severity"])
        assertEquals("On Schedule", result["flightStatus"])
        assertTrue((result["message"] as String).contains("Smooth flying conditions"))
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
        assertEquals(true, result["hasDisruptionRisk"])
        assertEquals("Severe", result["severity"])
        assertEquals("Delays Likely", result["flightStatus"])
        assertTrue((result["reasons"] as List<*>).isNotEmpty())
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

        assertEquals(true, result["hasDisruptionRisk"])
        assertTrue((result["reasons"] as List<*>).any { (it as String).contains("fog", ignoreCase = true) })
    }

    @Test
    fun testPackingSuggestionCard_rainyDestination() {
        val current = OpenMeteoCurrentWeather(apparentTemperature = 15.0)
        val daily = OpenMeteoDailyForecast(
            temperature2mMax = listOf(17.0),
            temperature2mMin = listOf(12.0),
            precipitationProbabilityMax = listOf(80),
            uvIndexMax = listOf(3.0)
        )
        val result = TravelerRules.computePackingSuggestionCard(current, daily, null, "London")

        assertEquals("ok", result["status"])
        assertEquals("London", result["destination"])
        assertTrue((result["headline"] as String).contains("raincoat in London"))
        val items = result["items"] as List<*>
        assertTrue(items.any { (it as String).contains("Raincoat", ignoreCase = true) })
    }

    @Test
    fun testPackingSuggestionCard_coldDestination() {
        val current = OpenMeteoCurrentWeather(apparentTemperature = 2.0)
        val daily = OpenMeteoDailyForecast(
            temperature2mMax = listOf(4.0),
            temperature2mMin = listOf(-2.0),
            precipitationProbabilityMax = listOf(10),
            uvIndexMax = listOf(2.0)
        )
        val result = TravelerRules.computePackingSuggestionCard(current, daily, null, "Zurich")

        val items = result["items"] as List<*>
        assertTrue(items.any { (it as String).contains("thermal coat", ignoreCase = true) })
    }

    @Test
    fun testPackingSuggestionCard_sunnyHighUv() {
        val current = OpenMeteoCurrentWeather(apparentTemperature = 32.0)
        val daily = OpenMeteoDailyForecast(
            temperature2mMax = listOf(35.0),
            temperature2mMin = listOf(26.0),
            precipitationProbabilityMax = listOf(0),
            uvIndexMax = listOf(9.5)
        )
        val result = TravelerRules.computePackingSuggestionCard(current, daily, null, "Dubai")

        val items = result["items"] as List<*>
        assertTrue(items.any { (it as String).contains("sunscreen", ignoreCase = true) })
        assertTrue(items.any { (it as String).contains("cotton", ignoreCase = true) })
    }

    @Test
    fun testSavedDestinationsCard() {
        val result = TravelerRules.computeSavedDestinationsCard(19.076, 72.877)

        assertEquals("ok", result["status"])
        val destinations = result["destinations"] as List<*>
        assertEquals(4, destinations.size)
        val first = destinations[0] as Map<*, *>
        assertEquals("London", first["name"])
        assertEquals("UK", first["country"])
        assertNotNull(first["packingTip"])
    }
}
