package com.weatherengine.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    @Test
    fun testAqiInfo() {
        assertEquals("Good", Formatters.aqiInfo(1).label)
        assertEquals(1, Formatters.aqiInfo(1).level)

        assertEquals("Fair", Formatters.aqiInfo(2).label)
        assertEquals(2, Formatters.aqiInfo(2).level)

        assertEquals("Moderate", Formatters.aqiInfo(3).label)
        assertEquals(3, Formatters.aqiInfo(3).level)

        assertEquals("Poor", Formatters.aqiInfo(4).label)
        assertEquals(4, Formatters.aqiInfo(4).level)

        assertEquals("Very Poor", Formatters.aqiInfo(5).label)
        assertEquals(5, Formatters.aqiInfo(5).level)

        assertEquals("Unknown", Formatters.aqiInfo(0).label)
        assertEquals(0, Formatters.aqiInfo(0).level)

        assertEquals("Unknown", Formatters.aqiInfo(6).label)
        assertEquals("Unknown", Formatters.aqiInfo(null).label)
    }

    @Test
    fun testWindMsToKmh() {
        assertEquals(36.0, Formatters.windMsToKmh(10.0)!!, 0.01)
        assertEquals(18.0, Formatters.windMsToKmh(5.0)!!, 0.01)
        assertNull(Formatters.windMsToKmh(null))
        assertNull(Formatters.windMsToKmh(Double.NaN))
        assertNull(Formatters.windMsToKmh(Double.POSITIVE_INFINITY))
    }

    @Test
    fun testFormatClock() {
        assertEquals("06:15", Formatters.formatClock("2026-09-28T06:15:00Z"))
        assertEquals("—", Formatters.formatClock("not-a-date"))
        assertEquals("—", Formatters.formatClock(""))
        assertEquals("—", Formatters.formatClock(null))
    }

    @Test
    fun testMinutesAgo() {
        val now = 1000000L
        assertEquals(5L, Formatters.minutesAgo(now - (5 * 60 * 1000L), now))
        assertEquals(0L, Formatters.minutesAgo(now, now))
        assertEquals(0L, Formatters.minutesAgo(now + 10000L, now)) // future timestamp never negative
    }

    @Test
    fun testWidgetTitle() {
        assertEquals("Air Quality", Formatters.widgetTitle("aqi_card"))
        assertEquals("Humidity", Formatters.widgetTitle("humidity_card"))
        assertEquals("UV Index", Formatters.widgetTitle("uv_index_card"))
        assertEquals("Sunrise & Sunset", Formatters.widgetTitle("sunrise_sunset_card"))
        assertEquals("Wind Speed", Formatters.widgetTitle("wind_speed_card"))
        assertEquals("Heat Alert", Formatters.widgetTitle("heat_alert_card"))
        assertEquals("Traffic", Formatters.widgetTitle("traffic_card"))
        assertEquals("Visibility", Formatters.widgetTitle("visibility_card"))
        assertEquals("Storm & Fog Alert", Formatters.widgetTitle("storm_fog_alert_card"))

        assertEquals("Some New Thing", Formatters.widgetTitle("some_new_thing_card"))
        assertEquals("Pollen Count", Formatters.widgetTitle("pollen_count"))
    }
}
