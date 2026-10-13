package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GardenerRulesTest {

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun daily(
        precipSum: List<Double?> = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        precipProbMax: List<Int?> = listOf(10, 10, 10, 10, 10, 10, 10),
        tempMax: List<Double> = listOf(22.0, 23.0, 21.0, 20.0, 22.0, 24.0, 25.0),
        tempMin: List<Double> = listOf(12.0, 13.0, 11.0, 10.0, 12.0, 14.0, 15.0)
    ) = OpenMeteoDailyForecast(
        time = (1..7).map { "2026-10-${it.toString().padStart(2, '0')}" },
        weatherCode = listOf(0, 1, 0, 2, 1, 0, 0),
        temperature2mMax = tempMax,
        temperature2mMin = tempMin,
        precipitationProbabilityMax = precipProbMax,
        precipitationSum = precipSum,
        windSpeed10mMax = listOf(15.0, 20.0, 10.0, 18.0, 12.0, 14.0, 16.0),
        uvIndexMax = listOf(4.0, 5.0, 6.0, 3.0, 4.0, 5.0, 4.0),
        sunrise = listOf("06:00", "06:01", "06:02", "06:03", "06:04", "06:05", "06:06"),
        sunset = listOf("18:30", "18:29", "18:28", "18:27", "18:26", "18:25", "18:24")
    )

    private fun current(humidity: Int = 50, temp: Double = 22.0) = OpenMeteoCurrentWeather(
        temperature2m = temp,
        relativeHumidity2m = humidity,
        windSpeed10m = 15.0,
        windGusts10m = 25.0,
        weatherCode = 1,
        apparentTemperature = temp,
        precipitation = 0.0,
        isDay = 1
    )

    // ── Soil Moisture ─────────────────────────────────────────────────────────
    @Test
    fun `computeSoilMoistureCard - wet when heavy rainfall`() {
        val heavy = daily(precipSum = List(7) { 7.0 })  // 49 mm total
        val result = GardenerRules.computeSoilMoistureCard(heavy, current())
        assertEquals("Wet", result["level"])
        assertTrue((result["precip7dMm"] as Double) >= 40.0)
    }

    @Test
    fun `computeSoilMoistureCard - moist with moderate rainfall`() {
        val moderate = daily(precipSum = List(7) { 3.0 })  // 21 mm total
        val result = GardenerRules.computeSoilMoistureCard(moderate, current(humidity = 60))
        assertEquals("Moist", result["level"])
    }

    @Test
    fun `computeSoilMoistureCard - dry with low humidity and no rain`() {
        val dry = daily(precipSum = List(7) { 0.0 })
        val result = GardenerRules.computeSoilMoistureCard(dry, current(humidity = 30))
        assertEquals("Dry", result["level"])
    }

    @Test
    fun `computeSoilMoistureCard - normal conditions`() {
        val normal = daily(precipSum = List(7) { 1.0 })   // 7 mm — below moist threshold
        val result = GardenerRules.computeSoilMoistureCard(normal, current(humidity = 55))
        assertEquals("Normal", result["level"])
    }

    @Test
    fun `computeSoilMoistureCard - null daily gracefully returns Normal`() {
        val result = GardenerRules.computeSoilMoistureCard(null, null)
        assertNotNull(result["level"])
        assertNotNull(result["message"])
    }

    // ── Rainfall Prediction ───────────────────────────────────────────────────
    @Test
    fun `computeRainfallPredictionCard - very wet week`() {
        val wet = daily(precipSum = listOf(8.0, 6.0, 10.0, 5.0, 7.0, 9.0, 8.0))
        val result = GardenerRules.computeRainfallPredictionCard(wet)
        assertEquals(7, result["rainyDayCount"])
        assertTrue((result["outlook"] as String).contains("Very Wet", ignoreCase = true))
    }

    @Test
    fun `computeRainfallPredictionCard - dry week`() {
        val dry = daily(precipSum = List(7) { 0.0 })
        val result = GardenerRules.computeRainfallPredictionCard(dry)
        assertEquals(0, result["rainyDayCount"])
        assertTrue((result["outlook"] as String).contains("Dry", ignoreCase = true))
    }

    @Test
    fun `computeRainfallPredictionCard - some rain (1–2 days)`() {
        val some = daily(precipSum = listOf(5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
        val result = GardenerRules.computeRainfallPredictionCard(some)
        assertEquals(1, result["rainyDayCount"])
        assertTrue((result["outlook"] as String).contains("Some Rain", ignoreCase = true))
    }

    // ── Frost Alert ───────────────────────────────────────────────────────────
    @Test
    fun `computeFrostAlertCard - hard frost detected`() {
        val hard = daily(tempMin = listOf(5.0, 1.0, -3.0, 4.0, 6.0, 5.0, 7.0))
        val result = GardenerRules.computeFrostAlertCard(hard)
        assertTrue(result["alertActive"] as Boolean)
        assertEquals("Hard Frost", result["alertLevel"])
        assertEquals(1, result["hardFrostDayCount"])
    }

    @Test
    fun `computeFrostAlertCard - frost risk only`() {
        val frostRisk = daily(tempMin = listOf(5.0, 1.5, 3.0, 4.0, 6.0, 5.0, 7.0))
        val result = GardenerRules.computeFrostAlertCard(frostRisk)
        assertTrue(result["alertActive"] as Boolean)
        assertEquals("Frost Risk", result["alertLevel"])
        assertEquals(0, result["hardFrostDayCount"])
        assertTrue((result["frostRiskDayCount"] as Int) >= 1)
    }

    @Test
    fun `computeFrostAlertCard - no frost`() {
        val clear = daily(tempMin = List(7) { 10.0 })
        val result = GardenerRules.computeFrostAlertCard(clear)
        assertFalse(result["alertActive"] as Boolean)
        assertEquals("Clear", result["alertLevel"])
    }

    // ── Planting Guidance ─────────────────────────────────────────────────────
    @Test
    fun `computePlantingGuidanceCard - good conditions`() {
        val goodDaily = daily(
            precipSum = listOf(2.0, 3.0, 1.0, 2.0, 1.0, 2.0, 2.0),  // 13 mm
            tempMax = List(7) { 22.0 },
            tempMin = List(7) { 12.0 }
        )
        val result = GardenerRules.computePlantingGuidanceCard(goodDaily, current())
        assertFalse(result["hasFrostRisk"] as Boolean)
        assertTrue((result["guidance"] as String).contains("Good", ignoreCase = true)
            || (result["guidance"] as String).contains("acceptable", ignoreCase = true))
    }

    @Test
    fun `computePlantingGuidanceCard - blocked by frost`() {
        val frostyDaily = daily(tempMin = listOf(0.0, 1.0, -1.0, 4.0, 5.0, 6.0, 7.0))
        val result = GardenerRules.computePlantingGuidanceCard(frostyDaily, current())
        assertTrue(result["hasFrostRisk"] as Boolean)
        assertTrue((result["guidance"] as String).contains("Avoid", ignoreCase = true))
    }

    @Test
    fun `computePlantingGuidanceCard - too hot`() {
        val hotDaily = daily(tempMax = List(7) { 36.0 }, tempMin = List(7) { 24.0 })
        val result = GardenerRules.computePlantingGuidanceCard(hotDaily, current(temp = 36.0))
        assertTrue((result["guidance"] as String).contains("Mulch", ignoreCase = true)
            || (result["guidance"] as String).contains("heat", ignoreCase = true))
    }
}
