package com.weatherengine.app.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSchoolCommuteFullPayload() {
        val jsonString = """
            {
                "status": "ok",
                "run": "morning",
                "date": "2026-10-12",
                "window": "07:00 - 08:00",
                "verdict": "prepare",
                "reasons": ["Rain likely (60%)", "Strong gusts 35 km/h"],
                "tips": ["Pack an umbrella or raincoat", "Secure loose items"]
            }
        """.trimIndent()
        val element = json.parseToJsonElement(jsonString)
        val result = WidgetMapper.mapWidget("school_commute_card", element)

        assertTrue(result is WidgetUi.SchoolCommute)
        val commute = result as WidgetUi.SchoolCommute
        assertEquals("school_commute_card", commute.type)
        assertEquals("morning", commute.run)
        assertEquals("2026-10-12", commute.date)
        assertEquals("07:00 - 08:00", commute.window)
        assertEquals("prepare", commute.verdict)
        assertEquals(listOf("Rain likely (60%)", "Strong gusts 35 km/h"), commute.reasons)
        assertEquals(listOf("Pack an umbrella or raincoat", "Secure loose items"), commute.tips)
    }

    @Test
    fun testRainAlertFullPayload() {
        val jsonString = """
            {
                "status": "ok",
                "message": "Rain likely around 4 PM",
                "peakProbabilityPercent": 75,
                "peakHourLabel": "4 PM"
            }
        """.trimIndent()
        val element = json.parseToJsonElement(jsonString)
        val result = WidgetMapper.mapWidget("rain_alert_card", element)

        assertTrue(result is WidgetUi.RainAlert)
        val rain = result as WidgetUi.RainAlert
        assertEquals("rain_alert_card", rain.type)
        assertEquals("Rain likely around 4 PM", rain.message)
        assertEquals(75, rain.peakProbabilityPercent)
        assertEquals("4 PM", rain.peakHourLabel)
    }

    @Test
    fun testSevereWeatherFullPayload() {
        val jsonString = """
            {
                "status": "ok",
                "alerts": [
                    {
                        "type": "thunderstorm",
                        "startsAt": "14:00",
                        "message": "Thunderstorm activity expected"
                    },
                    {
                        "type": "strong_wind",
                        "startsAt": "16:00",
                        "message": "Gusts up to 55 km/h"
                    }
                ],
                "message": "Severe weather expected in the next 24 hours"
            }
        """.trimIndent()
        val element = json.parseToJsonElement(jsonString)
        val result = WidgetMapper.mapWidget("severe_weather_card", element)

        assertTrue(result is WidgetUi.SevereWeather)
        val severe = result as WidgetUi.SevereWeather
        assertEquals("severe_weather_card", severe.type)
        assertEquals(2, severe.alerts.size)
        assertEquals("thunderstorm", severe.alerts[0].type)
        assertEquals("14:00", severe.alerts[0].startsAt)
        assertEquals("Thunderstorm activity expected", severe.alerts[0].message)
        assertEquals("strong_wind", severe.alerts[1].type)
        assertEquals("Severe weather expected in the next 24 hours", severe.message)
    }

    @Test
    fun testMissingOptionalFields() {
        // rain_alert_card without peakHourLabel
        val rainJson = """{"status":"ok","message":"No rain expected","peakProbabilityPercent":5}"""
        val rainResult = WidgetMapper.mapWidget("rain_alert_card", json.parseToJsonElement(rainJson))
        assertTrue(rainResult is WidgetUi.RainAlert)
        assertNull((rainResult as WidgetUi.RainAlert).peakHourLabel)

        // severe_weather_card without alerts list
        val severeJson = """{"status":"ok","message":"No severe weather expected in the next 24 hours"}"""
        val severeResult = WidgetMapper.mapWidget("severe_weather_card", json.parseToJsonElement(severeJson))
        assertTrue(severeResult is WidgetUi.SevereWeather)
        val severe = severeResult as WidgetUi.SevereWeather
        assertTrue(severe.alerts.isEmpty())
        assertEquals("No severe weather expected in the next 24 hours", severe.message)
    }

    @Test
    fun testMissingRequiredFieldsDegradesGracefully() {
        // school_commute_card missing 'verdict'
        val brokenCommuteJson = """{"status":"ok","run":"morning","date":"2026-10-12"}"""
        val commuteResult = WidgetMapper.mapWidget("school_commute_card", json.parseToJsonElement(brokenCommuteJson))
        assertTrue(commuteResult is WidgetUi.StatusOnly)
        assertEquals("error", (commuteResult as WidgetUi.StatusOnly).status)

        // rain_alert_card missing 'peakProbabilityPercent'
        val brokenRainJson = """{"status":"ok","message":"Rain soon"}"""
        val rainResult = WidgetMapper.mapWidget("rain_alert_card", json.parseToJsonElement(brokenRainJson))
        assertTrue(rainResult is WidgetUi.StatusOnly)
        assertEquals("error", (rainResult as WidgetUi.StatusOnly).status)
    }

    @Test
    fun testUnavailableAndErrorStatuses() {
        val unavailableJson = """{"status":"unavailable","message":"Available when online"}"""
        val element = json.parseToJsonElement(unavailableJson)

        val commute = WidgetMapper.mapWidget("school_commute_card", element)
        assertTrue(commute is WidgetUi.StatusOnly)
        assertEquals("unavailable", (commute as WidgetUi.StatusOnly).status)
        assertEquals("Available when online", commute.message)

        val errorJson = """{"status":"error","message":"Failed to compute commute"}"""
        val rain = WidgetMapper.mapWidget("rain_alert_card", json.parseToJsonElement(errorJson))
        assertTrue(rain is WidgetUi.StatusOnly)
        assertEquals("error", (rain as WidgetUi.StatusOnly).status)
        assertEquals("Failed to compute commute", rain.message)
    }

    @Test
    fun testWrongTypesAndNonObjectDegradation() {
        // peakProbabilityPercent is a string "abc" instead of int
        val wrongTypeJson = """{"status":"ok","message":"Test","peakProbabilityPercent":"not_a_number"}"""
        val rainResult = WidgetMapper.mapWidget("rain_alert_card", json.parseToJsonElement(wrongTypeJson))
        assertTrue(rainResult is WidgetUi.StatusOnly)
        assertEquals("error", (rainResult as WidgetUi.StatusOnly).status)

        // non-object json element (e.g. primitive)
        val primitiveElement = JsonPrimitive("just_a_string")
        val primitiveResult = WidgetMapper.mapWidget("school_commute_card", primitiveElement)
        assertTrue(primitiveResult is WidgetUi.StatusOnly)
        assertEquals("Invalid data format", (primitiveResult as WidgetUi.StatusOnly).message)
    }

    @Test
    fun testPollenCardMapping() {
        val pollenJson = """
            {
                "status": "ok",
                "pollenType": "Birch",
                "grainsCount": 65,
                "level": "High",
                "message": "Birch pollen is High (65 grains/m³)"
            }
        """.trimIndent()
        val result = WidgetMapper.mapWidget("pollen_card", json.parseToJsonElement(pollenJson))
        assertTrue(result is WidgetUi.Pollen)
        val pollen = result as WidgetUi.Pollen
        assertEquals("pollen_card", pollen.type)
        assertEquals("Birch", pollen.pollenType)
        assertEquals(65, pollen.grainsCount)
        assertEquals("High", pollen.level)
        assertEquals("Birch pollen is High (65 grains/m³)", pollen.message)
    }
}
