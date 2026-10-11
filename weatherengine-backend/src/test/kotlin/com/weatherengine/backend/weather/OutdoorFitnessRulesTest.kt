package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OutdoorFitnessRulesTest {

    @Test
    fun testSunriseSunsetCard_daylight() {
        val daily = OpenMeteoDailyForecast(
            sunrise = listOf("2026-10-11T06:15"),
            sunset = listOf("2026-10-11T18:30")
        )
        val now = LocalDateTime.parse("2026-10-11T10:00")
        val result = OutdoorFitnessRules.computeSunriseSunsetCard(daily, now)

        assertEquals("ok", result["status"])
        assertEquals("Daylight", result["daylightState"])
        assertTrue((result["message"] as String).contains("daylight remaining"))
    }

    @Test
    fun testSunriseSunsetCard_beforeSunrise() {
        val daily = OpenMeteoDailyForecast(
            sunrise = listOf("2026-10-11T06:15"),
            sunset = listOf("2026-10-11T18:30")
        )
        val now = LocalDateTime.parse("2026-10-11T05:00")
        val result = OutdoorFitnessRules.computeSunriseSunsetCard(daily, now)

        assertEquals("ok", result["status"])
        assertEquals("BeforeSunrise", result["daylightState"])
        assertTrue((result["message"] as String).contains("until sunrise"))
    }

    @Test
    fun testSunriseSunsetCard_afterSunset() {
        val daily = OpenMeteoDailyForecast(
            sunrise = listOf("2026-10-11T06:15"),
            sunset = listOf("2026-10-11T18:30")
        )
        val now = LocalDateTime.parse("2026-10-11T20:00")
        val result = OutdoorFitnessRules.computeSunriseSunsetCard(daily, now)

        assertEquals("ok", result["status"])
        assertEquals("AfterSunset", result["daylightState"])
        assertTrue((result["message"] as String).contains("Sunset was at"))
    }

    @Test
    fun testWindSpeedCard() {
        val current = OpenMeteoCurrentWeather(
            windSpeed10m = 12.0,
            windGusts10m = 18.0
        )
        val result = OutdoorFitnessRules.computeWindSpeedCard(current)

        assertEquals("ok", result["status"])
        assertEquals(12.0, result["speedKmh"])
        assertEquals("Light Breeze", result["category"])
    }

    @Test
    fun testHeatAlertCard_extremeHeat() {
        val current = OpenMeteoCurrentWeather(apparentTemperature = 40.0)
        val hourly = OpenMeteoHourlyForecast(
            apparentTemperature = listOf(40.0, 41.0, 39.0)
        )
        val result = OutdoorFitnessRules.computeHeatAlertCard(current, hourly)

        assertEquals("ok", result["status"])
        assertEquals(true, result["alertActive"])
        assertEquals("Warning", result["severity"])
        assertTrue((result["message"] as String).contains("Extreme Heat Alert"))
    }

    @Test
    fun testHeatAlertCard_normal() {
        val current = OpenMeteoCurrentWeather(apparentTemperature = 24.0)
        val hourly = OpenMeteoHourlyForecast(
            apparentTemperature = listOf(24.0, 25.0, 22.0)
        )
        val result = OutdoorFitnessRules.computeHeatAlertCard(current, hourly)

        assertEquals("ok", result["status"])
        assertEquals(false, result["alertActive"])
        assertEquals("Normal", result["severity"])
    }

    @Test
    fun testBestRunningHours_optimal() {
        val times = listOf(
            "2026-10-11T06:00",
            "2026-10-11T07:00",
            "2026-10-11T08:00"
        )
        val hourly = OpenMeteoHourlyForecast(
            time = times,
            apparentTemperature = listOf(18.0, 19.0, 20.0),
            precipitationProbability = listOf(0, 5, 10),
            windSpeed10m = listOf(8.0, 10.0, 12.0),
            weatherCode = listOf(0, 0, 1)
        )
        val now = LocalDateTime.parse("2026-10-11T05:30")
        val result = OutdoorFitnessRules.computeBestRunningHours(hourly, now)

        assertEquals("ok", result["status"])
        assertEquals(true, result["hasOptimalWindow"])
        assertNotNull(result["window"])
        assertTrue((result["score"] as Int) >= 80)
    }
}
