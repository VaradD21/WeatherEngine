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

    @Test
    fun testOutdoorFitnessWidgetsMapping() {
        val sunJson = """{"status":"ok","sunrise":"06:15 AM","sunset":"06:30 PM","daylightState":"Daylight","message":"Sun is up"}"""
        val sunResult = WidgetMapper.mapWidget("sunrise_sunset_card", json.parseToJsonElement(sunJson))
        assertTrue(sunResult is WidgetUi.SunriseSunset)
        assertEquals("06:15 AM", (sunResult as WidgetUi.SunriseSunset).sunrise)

        val runJson = """{"status":"ok","hasOptimalWindow":true,"window":"6 AM – 8 AM","score":85,"reasons":["Dry conditions"]}"""
        val runResult = WidgetMapper.mapWidget("best_running_hours_card", json.parseToJsonElement(runJson))
        assertTrue(runResult is WidgetUi.BestRunning)
        val run = runResult as WidgetUi.BestRunning
        assertTrue(run.hasOptimalWindow)
        assertEquals(85, run.score)

        val windJson = """{"status":"ok","speedKmh":15.0,"speedMetersPerSecond":4.2,"category":"Light Breeze","message":"Gentle breeze"}"""
        val windResult = WidgetMapper.mapWidget("wind_speed_card", json.parseToJsonElement(windJson))
        assertTrue(windResult is WidgetUi.Wind)
        assertEquals(15.0, (windResult as WidgetUi.Wind).speedKmh, 0.01)

        val heatJson = """{"status":"ok","alertActive":true,"severity":"Caution","temperatureCelsius":33.0,"message":"Heat caution"}"""
        val heatResult = WidgetMapper.mapWidget("heat_alert_card", json.parseToJsonElement(heatJson))
        assertTrue(heatResult is WidgetUi.HeatAlert)
        val heat = heatResult as WidgetUi.HeatAlert
        assertTrue(heat.alertActive)
        assertEquals("Caution", heat.severity)
    }

    @Test
    fun testBeachgoerWidgetsMapping() {
        val waveJson = """
            {
                "status": "ok",
                "isCoastal": true,
                "waveHeightMeters": 1.4,
                "wavePeriodSeconds": 9.5,
                "swellHeightMeters": 1.1,
                "category": "Moderate Waves",
                "surfRating": "Good for Surfing / Bodyboarding",
                "message": "Waves 1.4 m (Moderate Waves)"
            }
        """.trimIndent()
        val waveResult = WidgetMapper.mapWidget("wave_height_card", json.parseToJsonElement(waveJson))
        assertTrue(waveResult is WidgetUi.WaveHeight)
        val wave = waveResult as WidgetUi.WaveHeight
        assertEquals(1.4, wave.waveHeightMeters, 0.01)
        assertEquals(9.5, wave.wavePeriodSeconds, 0.01)
        assertEquals("Moderate Waves", wave.category)
        assertEquals("Good for Surfing / Bodyboarding", wave.surfRating)

        val seaJson = """
            {
                "status": "ok",
                "waterTempCelsius": 25.5,
                "isEstimated": false,
                "comfort": "Warm — Perfect for Swimming",
                "message": "Sea water temperature is 25.5°C"
            }
        """.trimIndent()
        val seaResult = WidgetMapper.mapWidget("sea_temp_card", json.parseToJsonElement(seaJson))
        assertTrue(seaResult is WidgetUi.SeaTemp)
        val sea = seaResult as WidgetUi.SeaTemp
        assertEquals(25.5, sea.waterTempCelsius, 0.01)
        assertEquals("Warm — Perfect for Swimming", sea.comfort)

        val tideJson = """
            {
                "status": "ok",
                "date": "2026-10-12",
                "nextTideState": "High Tide",
                "nextTideTime": "06:45 PM",
                "secondaryTideState": "Low Tide",
                "secondaryTideTime": "11:55 PM",
                "tideSummary": "High Tide expected around 06:45 PM",
                "message": "High Tide at 06:45 PM"
            }
        """.trimIndent()
        val tideResult = WidgetMapper.mapWidget("tide_card", json.parseToJsonElement(tideJson))
        assertTrue(tideResult is WidgetUi.Tide)
        val tide = tideResult as WidgetUi.Tide
        assertEquals("High Tide", tide.nextTideState)
        assertEquals("06:45 PM", tide.nextTideTime)
        assertEquals("Low Tide", tide.secondaryTideState)
    }

    @Test
    fun testTravelerWidgetsMapping() {
        val destJson = """
            {
                "status": "ok",
                "destinations": [
                    {
                        "id": "dest_lon",
                        "name": "London",
                        "country": "UK",
                        "lat": 51.5074,
                        "lon": -0.1278,
                        "tempCelsius": 14.0,
                        "condition": "Light Rain",
                        "weatherCode": 61,
                        "packingTip": "Carry a raincoat in London"
                    }
                ],
                "message": "Saved destinations"
            }
        """.trimIndent()
        val destResult = WidgetMapper.mapWidget("saved_destinations_card", json.parseToJsonElement(destJson))
        assertTrue(destResult is WidgetUi.SavedDestinations)
        val dest = destResult as WidgetUi.SavedDestinations
        assertEquals(1, dest.destinations.size)
        assertEquals("London", dest.destinations[0].name)
        assertEquals("Carry a raincoat in London", dest.destinations[0].packingTip)

        val flightJson = """
            {
                "status": "ok",
                "hasDisruptionRisk": true,
                "severity": "Warning",
                "flightStatus": "Delays Likely",
                "reasons": ["Strong crosswinds up to 58 km/h"],
                "message": "Moderate flight delay risk"
            }
        """.trimIndent()
        val flightResult = WidgetMapper.mapWidget("flight_alert_card", json.parseToJsonElement(flightJson))
        assertTrue(flightResult is WidgetUi.FlightAlert)
        val flight = flightResult as WidgetUi.FlightAlert
        assertTrue(flight.hasDisruptionRisk)
        assertEquals("Warning", flight.severity)
        assertEquals("Delays Likely", flight.flightStatus)
        assertEquals(listOf("Strong crosswinds up to 58 km/h"), flight.reasons)

        val packJson = """
            {
                "status": "ok",
                "destination": "London",
                "headline": "Carry a raincoat in London",
                "items": ["Raincoat or compact umbrella"],
                "tips": ["Carry a raincoat or umbrella (75% chance of rain in London)"],
                "tempRange": "12°C - 17°C",
                "rainChancePercent": 75,
                "uvIndexMax": 3.0,
                "message": "Carry a raincoat in London • 12°C - 17°C"
            }
        """.trimIndent()
        val packResult = WidgetMapper.mapWidget("packing_suggestion_card", json.parseToJsonElement(packJson))
        assertTrue(packResult is WidgetUi.PackingSuggestion)
        val pack = packResult as WidgetUi.PackingSuggestion
        assertEquals("London", pack.destination)
        assertEquals("Carry a raincoat in London", pack.headline)
        assertEquals(75, pack.rainChancePercent)
        assertTrue(pack.items.contains("Raincoat or compact umbrella"))
    }
}
