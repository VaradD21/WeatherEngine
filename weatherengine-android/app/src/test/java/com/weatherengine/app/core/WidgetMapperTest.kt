package com.weatherengine.app.core

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMapperTest {

    @Test
    fun testAllNineWidgets_ValidPayload() {
        // 1. aqi_card
        val aqiJson = buildJsonObject { put("aqi", 3); put("category", "Moderate") }
        val aqi = WidgetMapper.mapWidget("aqi_card", aqiJson)
        assertTrue(aqi is WidgetUi.Aqi)
        assertEquals(3, (aqi as WidgetUi.Aqi).aqi)
        assertEquals("Moderate", aqi.category)

        // 2. humidity_card
        val humJson = buildJsonObject { put("humidityPercent", 75) }
        val hum = WidgetMapper.mapWidget("humidity_card", humJson)
        assertTrue(hum is WidgetUi.Humidity)
        assertEquals(75, (hum as WidgetUi.Humidity).humidityPercent)

        // 3. uv_index_card
        val uvJson = buildJsonObject { put("uvIndex", 6.5); put("status", "High"); put("message", "Wear sun protection") }
        val uv = WidgetMapper.mapWidget("uv_index_card", uvJson)
        assertTrue(uv is WidgetUi.Uv)
        assertEquals(6.5, (uv as WidgetUi.Uv).uvIndex!!, 0.01)

        // 4. sunrise_sunset_card
        val sunJson = buildJsonObject { put("sunrise", "2026-09-28T06:00:00Z"); put("sunset", "2026-09-28T18:00:00Z") }
        val sun = WidgetMapper.mapWidget("sunrise_sunset_card", sunJson)
        assertTrue(sun is WidgetUi.SunriseSunset)
        assertEquals("2026-09-28T06:00:00Z", (sun as WidgetUi.SunriseSunset).sunrise)

        // 5. wind_speed_card
        val windJson = buildJsonObject { put("speedMetersPerSecond", 8.2) }
        val wind = WidgetMapper.mapWidget("wind_speed_card", windJson)
        assertTrue(wind is WidgetUi.Wind)
        assertEquals(8.2, (wind as WidgetUi.Wind).speedMetersPerSecond, 0.01)

        // 6. heat_alert_card
        val heatJson = buildJsonObject { put("alertActive", true); put("temperatureCelsius", 41.5); put("message", "Extreme Heat") }
        val heat = WidgetMapper.mapWidget("heat_alert_card", heatJson)
        assertTrue(heat is WidgetUi.HeatAlert)
        assertTrue((heat as WidgetUi.HeatAlert).alertActive)

        // 7. traffic_card
        val trafJson = buildJsonObject { put("status", "active"); put("message", "Road clear") }
        val traf = WidgetMapper.mapWidget("traffic_card", trafJson)
        assertTrue(traf is WidgetUi.Traffic)
        assertEquals("active", (traf as WidgetUi.Traffic).status)

        // 8. visibility_card
        val visJson = buildJsonObject { put("visibilityMeters", 5000); put("category", "Moderate") }
        val vis = WidgetMapper.mapWidget("visibility_card", visJson)
        assertTrue(vis is WidgetUi.Visibility)
        assertEquals(5000, (vis as WidgetUi.Visibility).visibilityMeters)

        // 9. storm_fog_alert_card
        val stormJson = buildJsonObject { put("alertActive", false); put("conditionCode", 800); put("message", "Clear") }
        val storm = WidgetMapper.mapWidget("storm_fog_alert_card", stormJson)
        assertTrue(storm is WidgetUi.StormFog)
        assertEquals(800, (storm as WidgetUi.StormFog).conditionCode)
    }

    @Test
    fun testMissingRequiredField_MapsToStatusOnly() {
        // Missing "category" in aqi_card
        val brokenAqi = buildJsonObject { put("aqi", 2) }
        val result = WidgetMapper.mapWidget("aqi_card", brokenAqi)
        assertTrue(result is WidgetUi.StatusOnly)
        assertEquals("error", (result as WidgetUi.StatusOnly).status)
        assertEquals("Temporarily unavailable", result.message)
    }

    @Test
    fun testWrongTypedField_MapsToStatusOnly() {
        // "humidityPercent" is string instead of int
        val brokenHum = buildJsonObject { put("humidityPercent", "high") }
        val result = WidgetMapper.mapWidget("humidity_card", brokenHum)
        assertTrue(result is WidgetUi.StatusOnly)
        assertEquals("error", (result as WidgetUi.StatusOnly).status)
    }

    @Test
    fun testDataStatusUnavailable_MapsToStatusOnly() {
        val unavailableData = buildJsonObject {
            put("status", "unavailable")
            put("message", "Air quality data unavailable")
        }
        val result = WidgetMapper.mapWidget("aqi_card", unavailableData)
        assertTrue(result is WidgetUi.StatusOnly)
        assertEquals("unavailable", (result as WidgetUi.StatusOnly).status)
        assertEquals("Air quality data unavailable", result.message)
    }

    @Test
    fun testUnknownWidgetType_MapsToUnsupported() {
        val unknownData = buildJsonObject { put("foo", "bar") }
        val result = WidgetMapper.mapWidget("alien_widget_card", unknownData)
        assertTrue(result is WidgetUi.Unsupported)
        assertEquals("alien_widget_card", (result as WidgetUi.Unsupported).type)
    }
}
