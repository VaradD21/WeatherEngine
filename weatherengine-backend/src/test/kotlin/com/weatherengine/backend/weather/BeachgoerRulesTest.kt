package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BeachgoerRulesTest {

    @Test
    fun testWaveHeightCard_coastalData() {
        val marine = OpenMeteoMarineResponse(
            hourly = OpenMeteoMarineHourly(
                time = listOf("2026-10-11T10:00", "2026-10-11T11:00"),
                waveHeight = listOf(1.5, 1.8),
                wavePeriod = listOf(10.5, 11.0),
                swellWaveHeight = listOf(1.2, 1.4),
                seaSurfaceTemperature = listOf(24.5, 24.6)
            )
        )
        val now = LocalDateTime.parse("2026-10-11T10:15")
        val result = BeachgoerRules.computeWaveHeightCard(marine, now)

        assertEquals("ok", result["status"])
        assertEquals(true, result["isCoastal"])
        assertEquals(1.5, result["waveHeightMeters"])
        assertEquals(10.5, result["wavePeriodSeconds"])
        assertEquals("Moderate Waves", result["category"])
        assertEquals("Great for Surfing", result["surfRating"])
    }

    @Test
    fun testWaveHeightCard_inlandFallback() {
        val now = LocalDateTime.parse("2026-10-11T10:15")
        val result = BeachgoerRules.computeWaveHeightCard(null, now)

        assertEquals("ok", result["status"])
        assertEquals(false, result["isCoastal"])
        assertEquals(0.0, result["waveHeightMeters"])
        assertEquals("Calm / Inland", result["category"])
        assertEquals("Flat", result["surfRating"])
    }

    @Test
    fun testSeaTempCard_marineReported() {
        val marine = OpenMeteoMarineResponse(
            hourly = OpenMeteoMarineHourly(
                time = listOf("2026-10-11T10:00"),
                waveHeight = listOf(1.0),
                wavePeriod = listOf(8.0),
                swellWaveHeight = listOf(0.8),
                seaSurfaceTemperature = listOf(23.2)
            )
        )
        val now = LocalDateTime.parse("2026-10-11T10:00")
        val result = BeachgoerRules.computeSeaTempCard(marine, null, now)

        assertEquals("ok", result["status"])
        assertEquals(23.2, result["waterTempCelsius"])
        assertEquals(false, result["isEstimated"])
        assertEquals("Pleasantly Cool — Comfortable", result["comfort"])
    }

    @Test
    fun testSeaTempCard_estimatedFallback() {
        val current = OpenMeteoCurrentWeather(apparentTemperature = 30.5)
        val now = LocalDateTime.parse("2026-10-11T10:00")
        val result = BeachgoerRules.computeSeaTempCard(null, current, now)

        assertEquals("ok", result["status"])
        assertEquals(28.0, result["waterTempCelsius"])
        assertEquals(true, result["isEstimated"])
        assertEquals("Warm — Perfect for Swimming", result["comfort"])
    }

    @Test
    fun testTideCard_morningWindow() {
        val now = LocalDateTime.parse("2026-10-11T08:30")
        val result = BeachgoerRules.computeTideCard(null, now)

        assertEquals("ok", result["status"])
        assertEquals("Low Tide", result["nextTideState"])
        assertEquals("12:30 PM", result["nextTideTime"])
        assertEquals("High Tide", result["secondaryTideState"])
        assertEquals("06:45 PM", result["secondaryTideTime"])
    }

    @Test
    fun testTideCard_eveningWindow() {
        val now = LocalDateTime.parse("2026-10-11T14:30")
        val result = BeachgoerRules.computeTideCard(null, now)

        assertEquals("ok", result["status"])
        assertEquals("High Tide", result["nextTideState"])
        assertEquals("06:45 PM", result["nextTideTime"])
    }
}
