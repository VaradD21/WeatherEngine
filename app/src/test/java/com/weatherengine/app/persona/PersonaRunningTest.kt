package com.weatherengine.app.persona

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaRunningTest {

    private fun generateDayHours(): List<HourlyRowUi> {
        return (0..23).map { h ->
            val hourStr = String.format("%02d", h)
            HourlyRowUi(
                timeIso = "2026-09-28T$hourStr:00",
                timeLabel = "$h:00",
                tempFormatted = "28°",
                apparentTempRaw = 30.0,
                condition = "Clear",
                iconKey = "clear_day",
                isDay = h in 6..18,
                precipProb = 0,
                windKmh = 10.0,
                uvIndex = 3.0,
                aqi = 40,
                weatherCode = 0
            )
        }
    }

    @Test
    fun testBestRunningWindow_CoolMorningWins() {
        val hours = generateDayHours().map { row ->
            val h = row.timeIso.substring(11, 13).toInt()
            when (h) {
                6, 7 -> row.copy(apparentTempRaw = 20.0, precipProb = 0, aqi = 30) // Ideal morning
                else -> row.copy(apparentTempRaw = 35.0, uvIndex = 9.0) // Hot rest of day
            }
        }

        val result = RunningRules.bestRunningWindow(hours)
        assertTrue(result is BestRunningResult.Optimal)
        val optimal = result as BestRunningResult.Optimal
        assertEquals("6 AM", optimal.startHourLabel)
        assertEquals("8 AM", optimal.endHourLabel)
        assertTrue(optimal.score >= 80)
        assertTrue(optimal.reasons.contains("cooler air") || optimal.reasons.contains("good air quality"))
    }

    @Test
    fun testBestRunningWindow_MiddayHeatAvoided() {
        val hours = generateDayHours().map { row ->
            val h = row.timeIso.substring(11, 13).toInt()
            when (h) {
                12, 13, 14 -> row.copy(apparentTempRaw = 38.0, uvIndex = 11.0) // Extreme midday heat
                17, 18 -> row.copy(apparentTempRaw = 22.0, uvIndex = 1.0) // Pleasant evening
                else -> row.copy(apparentTempRaw = 33.0)
            }
        }

        val result = RunningRules.bestRunningWindow(hours)
        assertTrue(result is BestRunningResult.Optimal)
        val optimal = result as BestRunningResult.Optimal
        assertEquals("5 PM", optimal.startHourLabel)
        assertEquals("7 PM", optimal.endHourLabel)
    }

    @Test
    fun testBestRunningWindow_RainyMorningPushesToDryHours() {
        val hours = generateDayHours().map { row ->
            val h = row.timeIso.substring(11, 13).toInt()
            when (h) {
                6, 7, 8 -> row.copy(apparentTempRaw = 20.0, precipProb = 90) // Rainy morning
                16, 17 -> row.copy(apparentTempRaw = 23.0, precipProb = 0) // Dry afternoon
                else -> row.copy(apparentTempRaw = 32.0, precipProb = 50)
            }
        }

        val result = RunningRules.bestRunningWindow(hours)
        assertTrue(result is BestRunningResult.Optimal)
        val optimal = result as BestRunningResult.Optimal
        assertEquals("4 PM", optimal.startHourLabel)
        assertEquals("6 PM", optimal.endHourLabel)
        assertTrue(optimal.reasons.contains("low rain chance"))
    }

    @Test
    fun testBestRunningWindow_AllHoursBad_ReturnsNoGoodWindow() {
        val hours = generateDayHours().map { row ->
            row.copy(apparentTempRaw = 40.0, precipProb = 80, aqi = 250) // Horrible weather all day
        }

        val result = RunningRules.bestRunningWindow(hours)
        assertTrue(result is BestRunningResult.NoGoodWindow)
        val noGood = result as BestRunningResult.NoGoodWindow
        assertTrue(noGood.reason.isNotBlank())
    }

    @Test
    fun testBestRunningWindow_ThunderstormScoresZero() {
        val hours = generateDayHours().map { row ->
            row.copy(apparentTempRaw = 20.0, weatherCode = 95) // Cool but thunderstorm
        }

        val result = RunningRules.bestRunningWindow(hours)
        assertTrue(result is BestRunningResult.NoGoodWindow)
        val noGood = result as BestRunningResult.NoGoodWindow
        assertEquals("Thunderstorms expected", noGood.reason)
    }

    @Test
    fun testBestRunningWindow_NullMetricsDoNotCrash() {
        val hours = generateDayHours().map { row ->
            row.copy(apparentTempRaw = null, windKmh = null, uvIndex = null, aqi = null, visibilityM = null)
        }

        val result = RunningRules.bestRunningWindow(hours)
        assertTrue(result is BestRunningResult.Optimal)
    }

    @Test
    fun testBestRunningWindow_FewerThanTwoCandidateHours() {
        val fewHours = listOf(
            HourlyRowUi(
                timeIso = "2026-09-28T21:00",
                timeLabel = "9 PM",
                tempFormatted = "22°",
                condition = "Clear",
                iconKey = "clear_night"
            )
        )

        val result = RunningRules.bestRunningWindow(fewHours)
        assertTrue(result is BestRunningResult.NoGoodWindow)
        assertEquals("Not enough daylight hours remaining", (result as BestRunningResult.NoGoodWindow).reason)
    }
}
