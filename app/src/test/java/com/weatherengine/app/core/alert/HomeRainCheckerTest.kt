package com.weatherengine.app.core.alert

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRainCheckerTest {

    @Test
    fun testIsRainWeather_withRainCodes() {
        val rainCodes = listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
        for (code in rainCodes) {
            assertTrue("Expected rain for code $code", HomeRainChecker.isRainWeather(code, 0.0))
        }
    }

    @Test
    fun testIsRainWeather_withClearCodes() {
        val clearCodes = listOf(0, 1, 2, 3, 45, 48, 71, 73, 75)
        for (code in clearCodes) {
            assertFalse("Expected no rain for code $code", HomeRainChecker.isRainWeather(code, 0.0))
        }
    }

    @Test
    fun testIsRainWeather_withPrecipitationThreshold() {
        assertTrue(HomeRainChecker.isRainWeather(0, 0.5))
        assertTrue(HomeRainChecker.isRainWeather(null, 1.2))
        assertFalse(HomeRainChecker.isRainWeather(0, 0.0))
        assertFalse(HomeRainChecker.isRainWeather(0, 0.05))
        assertFalse(HomeRainChecker.isRainWeather(null, null))
    }

    @Test
    fun testDescribeRain() {
        assertTrue(HomeRainChecker.describeRain(61, 1.5).contains("Light rain"))
        assertTrue(HomeRainChecker.describeRain(65, 12.0).contains("Heavy torrential rain"))
        assertTrue(HomeRainChecker.describeRain(95, 8.0).contains("Thunderstorm with rain"))
        assertTrue(HomeRainChecker.describeRain(51, 0.2).contains("Light drizzle"))
        assertEquals("Clear / No rain", HomeRainChecker.describeRain(0, 0.0))
    }
}
