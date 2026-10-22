package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BeachgoerRulesTest {

    @Test
    fun testWaveHeightCard_inlandFallback() {
        val now = LocalDateTime.parse("2026-10-11T10:00")
        val result = BeachgoerRules.computeWaveHeightCard(null, now)

        assertEquals("ok", result["status"])
        assertEquals(false, result["isCoastal"])
        assertEquals(0.0, result["waveHeightMeters"])
        assertEquals("Calm / Inland", result["category"])
    }

    @Test
    fun testWaveHeightCard_validCoastalData() {
        val marine = OpenMeteoMarineResponse(
            hourly = OpenMeteoMarineHourly(
                time = listOf("2026-10-11T10:00"),
                waveHeight = listOf(1.5),
                wavePeriod = listOf(10.0),
                swellWaveHeight = listOf(1.2)
            )
        )
        val now = LocalDateTime.parse("2026-10-11T10:00")
        val result = BeachgoerRules.computeWaveHeightCard(marine, now)

        assertEquals("ok", result["status"])
        assertEquals(true, result["isCoastal"])
        assertEquals(1.5, result["waveHeightMeters"])
        assertEquals(10.0, result["wavePeriodSeconds"])
        assertEquals("Moderate Waves", result["category"])
        assertEquals("Great for Surfing", result["surfRating"])
    }

    @Test
    fun testSeaTempCard_marineStationData() {
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
        assertEquals(true, result["estimated"])
        assertEquals(
            "Tide times are estimated based on lunar cycles and should not be used for navigation.",
            result["disclaimer"]
        )
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
        assertEquals(true, result["estimated"])
        assertEquals(
            "Tide times are estimated based on lunar cycles and should not be used for navigation.",
            result["disclaimer"]
        )
        assertEquals("High Tide", result["nextTideState"])
        assertEquals("06:45 PM", result["nextTideTime"])
    }
}
