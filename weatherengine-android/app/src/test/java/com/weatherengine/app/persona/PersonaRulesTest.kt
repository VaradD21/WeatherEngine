package com.weatherengine.app.persona

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaRulesTest {

    @Test
    fun testUvCategoryBoundaries() {
        assertEquals("Low", PersonaRules.uvCategory(2.0))
        assertEquals("Moderate", PersonaRules.uvCategory(3.0))
        assertEquals("Moderate", PersonaRules.uvCategory(5.0))
        assertEquals("High", PersonaRules.uvCategory(6.0))
        assertEquals("High", PersonaRules.uvCategory(7.0))
        assertEquals("Very high", PersonaRules.uvCategory(8.0))
        assertEquals("Very high", PersonaRules.uvCategory(10.0))
        assertEquals("Extreme", PersonaRules.uvCategory(11.0))
        assertEquals("Unavailable", PersonaRules.uvCategory(null))
        assertEquals("Unavailable", PersonaRules.uvCategory(-1.0))
        assertEquals("Unavailable", PersonaRules.uvCategory(25.0))
    }

    @Test
    fun testHumidityComfortBoundaries() {
        assertEquals("Dry", PersonaRules.humidityComfort(29))
        assertEquals("Comfortable", PersonaRules.humidityComfort(30))
        assertEquals("Comfortable", PersonaRules.humidityComfort(60))
        assertEquals("Humid", PersonaRules.humidityComfort(61))
        assertEquals("Humid", PersonaRules.humidityComfort(80))
        assertEquals("Very humid", PersonaRules.humidityComfort(81))
        assertEquals("Unavailable", PersonaRules.humidityComfort(null))
        assertEquals("Unavailable", PersonaRules.humidityComfort(-5))
        assertEquals("Unavailable", PersonaRules.humidityComfort(105))
    }

    @Test
    fun testUsAqiCategoryBoundaries() {
        assertEquals("Good", PersonaRules.usAqiCategory(50))
        assertEquals("Moderate", PersonaRules.usAqiCategory(51))
        assertEquals("Moderate", PersonaRules.usAqiCategory(100))
        assertEquals("Unhealthy for sensitive groups", PersonaRules.usAqiCategory(101))
        assertEquals("Unhealthy for sensitive groups", PersonaRules.usAqiCategory(150))
        assertEquals("Unhealthy", PersonaRules.usAqiCategory(151))
        assertEquals("Unhealthy", PersonaRules.usAqiCategory(200))
        assertEquals("Very unhealthy", PersonaRules.usAqiCategory(201))
        assertEquals("Very unhealthy", PersonaRules.usAqiCategory(300))
        assertEquals("Hazardous", PersonaRules.usAqiCategory(301))
        assertEquals("Unavailable", PersonaRules.usAqiCategory(null))
        assertEquals("Unavailable", PersonaRules.usAqiCategory(-1))
    }

    @Test
    fun testVisibilityCategoryBoundaries() {
        assertEquals("Very poor", PersonaRules.visibilityCategory(999.0))
        assertEquals("Poor", PersonaRules.visibilityCategory(1000.0))
        assertEquals("Poor", PersonaRules.visibilityCategory(3999.0))
        assertEquals("Moderate", PersonaRules.visibilityCategory(4000.0))
        assertEquals("Moderate", PersonaRules.visibilityCategory(9999.0))
        assertEquals("Good", PersonaRules.visibilityCategory(10000.0))
        assertEquals("Unavailable", PersonaRules.visibilityCategory(null))
        assertEquals("Unavailable", PersonaRules.visibilityCategory(-10.0))
    }

    @Test
    fun testWindCategoryBoundaries() {
        assertEquals("Light", PersonaRules.windCategory(11.0))
        assertEquals("Breezy", PersonaRules.windCategory(12.0))
        assertEquals("Breezy", PersonaRules.windCategory(28.0))
        assertEquals("Strong", PersonaRules.windCategory(29.0))
        assertEquals("Strong", PersonaRules.windCategory(49.0))
        assertEquals("Very strong", PersonaRules.windCategory(50.0))
        assertEquals("Unavailable", PersonaRules.windCategory(null))
        assertEquals("Unavailable", PersonaRules.windCategory(-2.0))
    }

    @Test
    fun testDaylightInfo() {
        // Before sunrise (05:00 vs sunrise 06:00, sunset 18:00)
        val before = PersonaRules.daylightInfo("2026-09-28T05:00", "2026-09-28T06:00", "2026-09-28T18:00")
        assertTrue(before is DaylightState.BeforeSunrise)
        assertEquals("1h 0m", (before as DaylightState.BeforeSunrise).timeUntilSunriseFormatted)
        assertEquals("12h 0m", before.dayLengthFormatted)

        // Midday (12:00 vs sunrise 06:00, sunset 18:00)
        val midday = PersonaRules.daylightInfo("2026-09-28T12:00", "2026-09-28T06:00", "2026-09-28T18:00")
        assertTrue(midday is DaylightState.Daylight)
        assertEquals("6h 0m", (midday as DaylightState.Daylight).timeUntilSunsetFormatted)

        // Exactly at sunset (18:00)
        val atSunset = PersonaRules.daylightInfo("2026-09-28T18:00", "2026-09-28T06:00", "2026-09-28T18:00")
        assertTrue(atSunset is DaylightState.AfterSunset)

        // After sunset (20:00)
        val after = PersonaRules.daylightInfo("2026-09-28T20:00", "2026-09-28T06:00", "2026-09-28T18:00")
        assertTrue(after is DaylightState.AfterSunset)

        // Invalid or null
        assertEquals(DaylightState.Unavailable, PersonaRules.daylightInfo(null, "2026-09-28T06:00", "2026-09-28T18:00"))
        assertEquals(DaylightState.Unavailable, PersonaRules.daylightInfo("invalid", "2026-09-28T06:00", "2026-09-28T18:00"))
    }

    @Test
    fun testHourlyWindow() {
        val sampleHours = (0..47).map { h ->
            val day = if (h < 24) "28" else "29"
            val hourStr = String.format("%02d", h % 24)
            HourlyRowUi(
                timeIso = "2026-09-${day}T$hourStr:00",
                timeLabel = "$h:00",
                tempFormatted = "25°",
                condition = "Clear",
                iconKey = "clear_day"
            )
        }

        // Normal: matches current time
        val win1 = PersonaRules.hourlyWindow(sampleHours, "2026-09-28T10:15", 24)
        assertEquals(24, win1.size)
        assertEquals("Now", win1.first().timeLabel)
        assertEquals("2026-09-28T10:00", win1.first().timeIso)

        // Current time not in list (earlier than first entry)
        val win2 = PersonaRules.hourlyWindow(sampleHours.drop(5), "2026-09-28T02:00", 24)
        assertEquals("Now", win2.first().timeLabel)
        assertEquals("2026-09-28T05:00", win2.first().timeIso)

        // Fewer than 24 rows left
        val win3 = PersonaRules.hourlyWindow(sampleHours.take(10), "2026-09-28T00:00", 24)
        assertEquals(10, win3.size)
        assertEquals("Now", win3.first().timeLabel)

        // Empty input
        assertTrue(PersonaRules.hourlyWindow(emptyList(), "2026-09-28T10:00", 24).isEmpty())
    }

    @Test
    fun testHeatStatus() {
        assertTrue(PersonaRules.heatStatus(31.9) is HeatStatus.None)
        assertTrue(PersonaRules.heatStatus(32.0) is HeatStatus.Caution)
        assertTrue(PersonaRules.heatStatus(35.9) is HeatStatus.Caution)
        assertTrue(PersonaRules.heatStatus(36.0) is HeatStatus.Alert)
        assertTrue(PersonaRules.heatStatus(null) is HeatStatus.None)
    }

    @Test
    fun testStormFogStatus() {
        val baseHours = (0..15).map { h ->
            val hourStr = String.format("%02d", h)
            HourlyRowUi(
                timeIso = "2026-09-28T$hourStr:00",
                timeLabel = "$h:00",
                tempFormatted = "25°",
                condition = "Clear",
                iconKey = "clear_day",
                weatherCode = 0,
                visibilityM = 10000.0
            )
        }

        // Clear
        val clearStatus = PersonaRules.stormFogStatus(baseHours, "2026-09-28T00:00")
        assertFalse(clearStatus.alertActive)

        // Thunderstorm at hour 3
        val stormHours = baseHours.mapIndexed { idx, row ->
            if (idx == 3) row.copy(weatherCode = 95) else row
        }
        val stormStatus = PersonaRules.stormFogStatus(stormHours, "2026-09-28T00:00")
        assertTrue(stormStatus.alertActive)
        assertTrue(stormStatus.message.contains("Thunderstorm"))

        // Fog at hour 2
        val fogHours = baseHours.mapIndexed { idx, row ->
            if (idx == 2) row.copy(weatherCode = 45) else row
        }
        val fogStatus = PersonaRules.stormFogStatus(fogHours, "2026-09-28T00:00")
        assertTrue(fogStatus.alertActive)
        assertTrue(fogStatus.message.contains("Fog"))

        // Low visibility < 1000m at hour 1
        val lowVisHours = baseHours.mapIndexed { idx, row ->
            if (idx == 1) row.copy(visibilityM = 800.0) else row
        }
        val lowVisStatus = PersonaRules.stormFogStatus(lowVisHours, "2026-09-28T00:00")
        assertTrue(lowVisStatus.alertActive)
    }

    @Test
    fun testCommuteRain() {
        val hours = (0..5).map { h ->
            HourlyRowUi(
                timeIso = "2026-09-28T0$h:00",
                timeLabel = "$h:00",
                tempFormatted = "25°",
                condition = "Cloudy",
                iconKey = "cloudy",
                precipProb = if (h == 2) 19 else 10
            )
        }

        // Threshold 19 -> Dry
        assertEquals("Dry commute expected", PersonaRules.commuteRain(hours).advice)

        // Threshold 20 -> Slight chance
        val slightHours = hours.mapIndexed { i, r -> if (i == 2) r.copy(precipProb = 20) else r }
        assertEquals("Slight chance of rain", PersonaRules.commuteRain(slightHours).advice)

        // Threshold 49 -> Slight chance
        val almostHighHours = hours.mapIndexed { i, r -> if (i == 2) r.copy(precipProb = 49) else r }
        assertEquals("Slight chance of rain", PersonaRules.commuteRain(almostHighHours).advice)

        // Threshold 50 -> Take an umbrella
        val highHours = hours.mapIndexed { i, r -> if (i == 2) r.copy(precipProb = 50) else r }
        val highResult = PersonaRules.commuteRain(highHours)
        assertEquals("Take an umbrella", highResult.advice)
        assertEquals(50, highResult.peakRainChance)

        // Empty list
        assertEquals("Dry commute expected", PersonaRules.commuteRain(emptyList()).advice)
    }

    @Test
    fun testMergedWidgets() {
        // Single persona
        val health = PersonaRules.mergedWidgets(setOf("health_conscious"))
        assertEquals(listOf(PersonaWidgetType.AQI, PersonaWidgetType.UV, PersonaWidgetType.HUMIDITY, PersonaWidgetType.POLLEN), health)

        // Two personas with no overlap
        val fitnessAndCommuter = PersonaRules.mergedWidgets(setOf("outdoor_fitness", "commuter"))
        assertEquals(8, fitnessAndCommuter.size)
        assertTrue(fitnessAndCommuter.contains(PersonaWidgetType.BEST_RUNNING))
        assertTrue(fitnessAndCommuter.contains(PersonaWidgetType.COMMUTE_RAIN))

        // Unknown code ignored
        val withUnknown = PersonaRules.mergedWidgets(setOf("unknown_code", "health_conscious"))
        assertEquals(health, withUnknown)

        // Empty selection
        assertTrue(PersonaRules.mergedWidgets(emptySet()).isEmpty())

        // Order preserved, no duplicates
        val allThree = PersonaRules.mergedWidgets(setOf("commuter", "outdoor_fitness", "health_conscious"))
        assertEquals(12, allThree.size)
        assertEquals(12, allThree.distinct().size)
        assertEquals(PersonaWidgetType.AQI, allThree.first())
        assertEquals(PersonaWidgetType.TRAFFIC, allThree.last())
    }
}
