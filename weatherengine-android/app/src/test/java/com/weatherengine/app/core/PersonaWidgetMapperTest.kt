package com.weatherengine.app.core

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaWidgetMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

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

    @Test
    fun testGardenerWidgetsMapping() {
        // soil_moisture_card
        val soilJson = """{
            "status":"ok","level":"Wet","precip7dMm":49.0,
            "humidityPercent":75,"advice":"Delay watering","message":"Wet soil — Delay watering"
        }"""
        val soilResult = WidgetMapper.mapWidget("soil_moisture_card", json.parseToJsonElement(soilJson))
        assertTrue(soilResult is WidgetUi.SoilMoisture)
        val soil = soilResult as WidgetUi.SoilMoisture
        assertEquals("Wet", soil.level)
        assertEquals(49.0, soil.precip7dMm, 0.01)
        assertEquals(75, soil.humidityPercent)

        // rainfall_prediction_card
        val rainJson = """{
            "status":"ok","totalMm":35.5,"maxProbabilityPercent":80,
            "rainyDayCount":5,"forecastDays":7,"outlook":"Rainy Week","message":"Rainy Week"
        }"""
        val rainResult = WidgetMapper.mapWidget("rainfall_prediction_card", json.parseToJsonElement(rainJson))
        assertTrue(rainResult is WidgetUi.RainfallPrediction)
        val rain = rainResult as WidgetUi.RainfallPrediction
        assertEquals(35.5, rain.totalMm, 0.01)
        assertEquals(80, rain.maxProbabilityPercent)
        assertEquals(5, rain.rainyDayCount)
        assertEquals("Rainy Week", rain.outlook)

        // frost_alert_card — alert active
        val frostJson = """{
            "status":"ok","alertActive":true,"alertLevel":"Hard Frost",
            "frostRiskDayCount":1,"hardFrostDayCount":1,
            "advice":"Cover plants immediately","message":"Hard frost tonight"
        }"""
        val frostResult = WidgetMapper.mapWidget("frost_alert_card", json.parseToJsonElement(frostJson))
        assertTrue(frostResult is WidgetUi.FrostAlert)
        val frost = frostResult as WidgetUi.FrostAlert
        assertTrue(frost.alertActive)
        assertEquals("Hard Frost", frost.alertLevel)
        assertEquals(1, frost.hardFrostDayCount)

        // frost_alert_card — no alert
        val clearFrostJson = """{
            "status":"ok","alertActive":false,"alertLevel":"Clear",
            "frostRiskDayCount":0,"hardFrostDayCount":0,
            "advice":"No frost risk","message":"No frost risk"
        }"""
        val clearFrost = WidgetMapper.mapWidget("frost_alert_card", json.parseToJsonElement(clearFrostJson))
        assertTrue(clearFrost is WidgetUi.FrostAlert)
        assertFalse((clearFrost as WidgetUi.FrostAlert).alertActive)

        // planting_guidance_card
        val plantJson = """{
            "status":"ok","hasFrostRisk":false,"avgMaxTempC":23.5,"avgMinTempC":13.0,
            "totalRainMm7d":14.0,"guidance":"Good planting conditions","message":"Good planting conditions"
        }"""
        val plantResult = WidgetMapper.mapWidget("planting_guidance_card", json.parseToJsonElement(plantJson))
        assertTrue(plantResult is WidgetUi.PlantingGuidance)
        val plant = plantResult as WidgetUi.PlantingGuidance
        assertFalse(plant.hasFrostRisk)
        assertEquals(23.5, plant.avgMaxTempC, 0.01)
        assertEquals(14.0, plant.totalRainMm7d, 0.01)
        assertTrue(plant.guidance.contains("Good", ignoreCase = true))
    }

    @Test
    fun testEventPlannerWidgetsMapping() {
        // extended_forecast_card
        val forecastJson = """{
            "status":"ok",
            "forecastDayCount":3,
            "days":[
                {"date":"2026-10-01","condition":"Clear Sky","maxTempC":25.0,"minTempC":16.0,"precipProbPercent":10},
                {"date":"2026-10-02","condition":"Rain","maxTempC":19.0,"minTempC":14.0,"precipProbPercent":75},
                {"date":"2026-10-03","condition":"Partly Cloudy","maxTempC":22.0,"minTempC":15.0,"precipProbPercent":30}
            ],
            "message":"3-day extended forecast available"
        }"""
        val forecastResult = WidgetMapper.mapWidget("extended_forecast_card", json.parseToJsonElement(forecastJson))
        assertTrue(forecastResult is WidgetUi.ExtendedForecast)
        val forecast = forecastResult as WidgetUi.ExtendedForecast
        assertEquals(3, forecast.forecastDayCount)
        assertEquals(3, forecast.days.size)
        assertEquals("Clear Sky", forecast.days[0].condition)
        assertEquals(25.0, forecast.days[0].maxTempC, 0.01)
        assertEquals(75, forecast.days[1].precipProbPercent)

        // rain_probability_card
        val rainProbJson = """{
            "status":"ok","peakEventHourProbPercent":65,"avgEventHourProbPercent":40,
            "daily3MaxProbPercent":70,"outlook":"Moderate rain chance during event hours",
            "message":"Moderate rain chance during event hours"
        }"""
        val rainProbResult = WidgetMapper.mapWidget("rain_probability_card", json.parseToJsonElement(rainProbJson))
        assertTrue(rainProbResult is WidgetUi.RainProbability)
        val rainProb = rainProbResult as WidgetUi.RainProbability
        assertEquals(65, rainProb.peakEventHourProbPercent)
        assertEquals(40, rainProb.avgEventHourProbPercent)
        assertEquals(70, rainProb.daily3MaxProbPercent)

        // comfort_index_card — excellent
        val comfortJson = """{
            "status":"ok","comfortIndex":82,"rating":"Excellent","tempCelsius":23.0,
            "apparentTempCelsius":22.5,"humidityPercent":48,"uvIndex":4.0,
            "advice":"Perfect outdoor conditions","message":"Excellent outdoor comfort (82/100)"
        }"""
        val comfortResult = WidgetMapper.mapWidget("comfort_index_card", json.parseToJsonElement(comfortJson))
        assertTrue(comfortResult is WidgetUi.ComfortIndex)
        val comfort = comfortResult as WidgetUi.ComfortIndex
        assertEquals(82, comfort.comfortIndex)
        assertEquals("Excellent", comfort.rating)
        assertEquals(23.0, comfort.tempCelsius, 0.01)
        assertEquals(48, comfort.humidityPercent)
        assertEquals(4.0, comfort.uvIndex, 0.01)
        assertTrue(comfort.advice.contains("Perfect", ignoreCase = true))

        // extended_forecast_card — empty days array
        val emptyForecastJson = """{"status":"ok","forecastDayCount":0,"days":[],"message":"No data"}"""
        val emptyResult = WidgetMapper.mapWidget("extended_forecast_card", json.parseToJsonElement(emptyForecastJson))
        assertTrue(emptyResult is WidgetUi.ExtendedForecast)
        assertTrue((emptyResult as WidgetUi.ExtendedForecast).days.isEmpty())
    }
}
