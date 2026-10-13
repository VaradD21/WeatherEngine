package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EventPlannerRulesTest {

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun daily(
        precipProbMax: List<Int?> = listOf(10, 20, 15, 30, 10, 5, 10),
        precipSum: List<Double?> = listOf(0.0, 1.0, 0.5, 2.0, 0.0, 0.0, 0.0),
        tempMax: List<Double> = listOf(24.0, 22.0, 23.0, 21.0, 25.0, 26.0, 24.0),
        tempMin: List<Double> = listOf(16.0, 15.0, 14.0, 13.0, 16.0, 17.0, 15.0)
    ) = OpenMeteoDailyForecast(
        time = (1..7).map { "2026-10-${it.toString().padStart(2, '0')}" },
        weatherCode = listOf(0, 1, 0, 2, 1, 0, 0),
        temperature2mMax = tempMax,
        temperature2mMin = tempMin,
        precipitationProbabilityMax = precipProbMax,
        precipitationSum = precipSum,
        windSpeed10mMax = listOf(10.0, 12.0, 8.0, 15.0, 10.0, 9.0, 11.0),
        uvIndexMax = listOf(5.0, 4.0, 6.0, 3.0, 5.0, 6.0, 4.0),
        sunrise = listOf("06:00", "06:01", "06:02", "06:03", "06:04", "06:05", "06:06"),
        sunset = listOf("18:30", "18:29", "18:28", "18:27", "18:26", "18:25", "18:24")
    )

    private fun current(temp: Double = 22.0, humidity: Int = 50, uv: Double = 4.0) =
        OpenMeteoCurrentWeather(
            temperature2m = temp,
            relativeHumidity2m = humidity,
            windSpeed10m = 10.0,
            windGusts10m = 20.0,
            weatherCode = 0,
            apparentTemperature = temp - 1,
            precipitation = 0.0,
            isDay = 1
        )

    private fun hourly(
        precipProb: List<Int?> = List(168) { if (it % 24 in 15..19) 20 else 5 }
    ) = OpenMeteoHourlyForecast(
        time = (0 until 168).map { h ->
            val day = h / 24 + 1
            val hour = h % 24
            "2026-10-${day.toString().padStart(2, '0')}T${hour.toString().padStart(2, '0')}:00"
        },
        temperature2m = List(168) { 22.0 },
        apparentTemperature = List(168) { 21.0 },
        relativeHumidity2m = List(168) { 50 },
        precipitationProbability = precipProb,
        precipitation = List(168) { 0.0 },
        weatherCode = List(168) { 0 },
        windSpeed10m = List(168) { 10.0 },
        windGusts10m = List(168) { 15.0 },
        visibility = List(168) { 10000.0 },
        uvIndex = List(168) { 3.0 },
        isDay = List(168) { 1 }
    )

    // ── Extended Forecast ─────────────────────────────────────────────────────
    @Test
    fun `computeExtendedForecastCard - returns 7 day entries`() {
        val result = EventPlannerRules.computeExtendedForecastCard(daily())
        assertEquals(7, result["forecastDayCount"])
        @Suppress("UNCHECKED_CAST")
        val days = result["days"] as List<Map<String, Any?>>
        assertEquals(7, days.size)
        assertTrue(days[0].containsKey("date"))
        assertTrue(days[0].containsKey("condition"))
        assertTrue(days[0].containsKey("maxTempC"))
        assertTrue(days[0].containsKey("precipProbPercent"))
    }

    @Test
    fun `computeExtendedForecastCard - null daily gracefully degrades`() {
        val result = EventPlannerRules.computeExtendedForecastCard(null)
        assertEquals(0, result["forecastDayCount"])
        assertNotNull(result["message"])
    }

    @Test
    fun `computeExtendedForecastCard - condition labels mapped correctly`() {
        val customDaily = OpenMeteoDailyForecast(
            time = listOf("2026-10-01"),
            weatherCode = listOf(95),  // Thunderstorm
            temperature2mMax = listOf(20.0),
            temperature2mMin = listOf(15.0),
            precipitationProbabilityMax = listOf(80),
            precipitationSum = listOf(10.0),
            windSpeed10mMax = listOf(25.0),
            uvIndexMax = listOf(2.0),
            sunrise = listOf("06:00"),
            sunset = listOf("18:00")
        )
        val result = EventPlannerRules.computeExtendedForecastCard(customDaily)
        @Suppress("UNCHECKED_CAST")
        val days = result["days"] as List<Map<String, Any?>>
        assertEquals("Thunderstorm", days[0]["condition"])
    }

    // ── Rain Probability ──────────────────────────────────────────────────────
    @Test
    fun `computeRainProbabilityCard - clear conditions`() {
        val lowProb = hourly(precipProb = List(168) { 5 })
        val result = EventPlannerRules.computeRainProbabilityCard(lowProb, daily())
        assertTrue((result["peakEventHourProbPercent"] as Int) < 25)
        assertTrue((result["outlook"] as String).contains("Clear", ignoreCase = true))
    }

    @Test
    fun `computeRainProbabilityCard - high rain during event hours`() {
        val highProb = hourly(precipProb = List(168) { h ->
            if ((h % 24) in 15..19) 75 else 10
        })
        val result = EventPlannerRules.computeRainProbabilityCard(highProb, daily())
        assertTrue((result["peakEventHourProbPercent"] as Int) >= 70)
        assertTrue((result["outlook"] as String).contains("High", ignoreCase = true))
    }

    @Test
    fun `computeRainProbabilityCard - null hourly falls back to daily`() {
        val result = EventPlannerRules.computeRainProbabilityCard(null, daily(precipProbMax = listOf(60, 70, 80)))
        assertNotNull(result["outlook"])
        // peakEventHourProbPercent would be 0 from hourly, daily3MaxProbPercent should be 80
        assertEquals(80, result["daily3MaxProbPercent"])
    }

    // ── Comfort Index ─────────────────────────────────────────────────────────
    @Test
    fun `computeComfortIndexCard - excellent comfort in ideal conditions`() {
        val result = EventPlannerRules.computeComfortIndexCard(current(temp = 23.0, humidity = 45), hourly(), daily(
            tempMax = List(7) { 23.0 }
        ))
        val score = result["comfortIndex"] as Int
        assertTrue(score >= EventPlannerConstants.COMFORT_GREAT_MIN,
            "Expected Excellent comfort (≥${EventPlannerConstants.COMFORT_GREAT_MIN}) but was $score")
        assertEquals("Excellent", result["rating"])
    }

    @Test
    fun `computeComfortIndexCard - poor comfort in extreme conditions`() {
        val result = EventPlannerRules.computeComfortIndexCard(
            current(temp = 42.0, humidity = 90),
            hourly(),
            daily(tempMax = List(7) { 42.0 })
        )
        val score = result["comfortIndex"] as Int
        assertTrue(score < EventPlannerConstants.COMFORT_GOOD_MIN,
            "Expected low comfort but got $score")
    }

    @Test
    fun `computeComfortIndexCard - fields present in output`() {
        val result = EventPlannerRules.computeComfortIndexCard(current(), hourly(), daily())
        assertTrue(result.containsKey("comfortIndex"))
        assertTrue(result.containsKey("rating"))
        assertTrue(result.containsKey("advice"))
        assertTrue(result.containsKey("tempCelsius"))
        assertTrue(result.containsKey("humidityPercent"))
        assertTrue(result.containsKey("uvIndex"))
        assertNotNull(result["message"])
    }

    @Test
    fun `computeComfortIndexCard - null inputs gracefully default`() {
        val result = EventPlannerRules.computeComfortIndexCard(null, null, null)
        assertNotNull(result["comfortIndex"])
        assertNotNull(result["rating"])
    }
}
