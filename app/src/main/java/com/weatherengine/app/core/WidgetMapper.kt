package com.weatherengine.app.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object WidgetMapper {

    fun mapWidget(type: String, dataElement: JsonElement?): WidgetUi {
        if (dataElement !is JsonObject) {
            return WidgetUi.StatusOnly(type = type, status = "error", message = "Invalid data format")
        }

        return try {
            val status = dataElement["status"]?.jsonPrimitive?.contentOrNull
            if (status == "error" || status == "unavailable") {
                val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Status: $status"
                return WidgetUi.StatusOnly(type = type, status = status, message = message)
            }

            when (type) {
                "aqi_card" -> {
                    val aqi = dataElement["aqi"]?.jsonPrimitive?.intOrNull ?: error("Missing aqi")
                    val category = dataElement["category"]?.jsonPrimitive?.contentOrNull ?: error("Missing category")
                    WidgetUi.Aqi(type = type, aqi = aqi, category = category)
                }
                "humidity_card" -> {
                    val humidity = dataElement["humidityPercent"]?.jsonPrimitive?.intOrNull ?: error("Missing humidityPercent")
                    WidgetUi.Humidity(type = type, humidityPercent = humidity)
                }
                "uv_index_card" -> {
                    val uvIndex = dataElement["uvIndex"]?.jsonPrimitive?.doubleOrNull
                    val uvStatus = dataElement["status"]?.jsonPrimitive?.contentOrNull
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull
                    WidgetUi.Uv(type = type, status = uvStatus, uvIndex = uvIndex, message = message)
                }
                "pollen_card" -> {
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Pollen count normal"
                    val pollenType = dataElement["pollenType"]?.jsonPrimitive?.contentOrNull ?: "Pollen"
                    val grains = dataElement["grainsCount"]?.jsonPrimitive?.intOrNull ?: 0
                    val level = dataElement["level"]?.jsonPrimitive?.contentOrNull ?: "Low"
                    WidgetUi.Pollen(type = type, pollenType = pollenType, grainsCount = grains, level = level, message = message)
                }
                "sunrise_sunset_card" -> {
                    val sunrise = dataElement["sunrise"]?.jsonPrimitive?.contentOrNull ?: error("Missing sunrise")
                    val sunset = dataElement["sunset"]?.jsonPrimitive?.contentOrNull ?: error("Missing sunset")
                    val daylightState = dataElement["daylightState"]?.jsonPrimitive?.contentOrNull ?: "Daylight"
                    val dayLength = dataElement["dayLength"]?.jsonPrimitive?.contentOrNull ?: ""
                    val timeUntil = dataElement["timeUntil"]?.jsonPrimitive?.contentOrNull ?: ""
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Sunrise: $sunrise • Sunset: $sunset"
                    WidgetUi.SunriseSunset(
                        type = type,
                        sunrise = sunrise,
                        sunset = sunset,
                        daylightState = daylightState,
                        dayLength = dayLength,
                        timeUntil = timeUntil,
                        message = message
                    )
                }
                "best_running_hours_card" -> {
                    val hasWindow = dataElement["hasOptimalWindow"]?.jsonPrimitive?.booleanOrNull ?: false
                    val window = dataElement["window"]?.jsonPrimitive?.contentOrNull
                    val score = dataElement["score"]?.jsonPrimitive?.intOrNull
                    val reasons = dataElement["reasons"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Running conditions calculated"
                    WidgetUi.BestRunning(
                        type = type,
                        hasOptimalWindow = hasWindow,
                        window = window,
                        score = score,
                        reasons = reasons,
                        message = message
                    )
                }
                "wind_speed_card" -> {
                    val speedMs = dataElement["speedMetersPerSecond"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val speedKmh = dataElement["speedKmh"]?.jsonPrimitive?.doubleOrNull ?: (speedMs * 3.6)
                    val gusts = dataElement["gustsKmh"]?.jsonPrimitive?.doubleOrNull ?: speedKmh
                    val category = dataElement["category"]?.jsonPrimitive?.contentOrNull ?: "Moderate"
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "$speedKmh km/h wind"
                    WidgetUi.Wind(
                        type = type,
                        speedKmh = speedKmh,
                        speedMetersPerSecond = speedMs,
                        gustsKmh = gusts,
                        category = category,
                        message = message
                    )
                }
                "heat_alert_card" -> {
                    val active = dataElement["alertActive"]?.jsonPrimitive?.booleanOrNull ?: false
                    val severity = dataElement["severity"]?.jsonPrimitive?.contentOrNull ?: if (active) "Caution" else "Normal"
                    val temp = dataElement["temperatureCelsius"]?.jsonPrimitive?.doubleOrNull ?: 25.0
                    val maxApparent = dataElement["maxApparentCelsius"]?.jsonPrimitive?.doubleOrNull
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "No heat risk"
                    WidgetUi.HeatAlert(
                        type = type,
                        alertActive = active,
                        severity = severity,
                        temperatureCelsius = temp,
                        maxApparentCelsius = maxApparent,
                        message = msg
                    )
                }
                "traffic_card" -> {
                    val tStatus = dataElement["status"]?.jsonPrimitive?.contentOrNull ?: error("Missing status")
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: error("Missing message")
                    WidgetUi.Traffic(type = type, status = tStatus, message = msg)
                }
                "visibility_card" -> {
                    val visibility = dataElement["visibilityMeters"]?.jsonPrimitive?.intOrNull ?: error("Missing visibilityMeters")
                    val category = dataElement["category"]?.jsonPrimitive?.contentOrNull ?: error("Missing category")
                    WidgetUi.Visibility(type = type, visibilityMeters = visibility, category = category)
                }
                "storm_fog_alert_card" -> {
                    val active = dataElement["alertActive"]?.jsonPrimitive?.booleanOrNull ?: error("Missing alertActive")
                    val code = dataElement["conditionCode"]?.jsonPrimitive?.intOrNull ?: error("Missing conditionCode")
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: error("Missing message")
                    WidgetUi.StormFog(type = type, alertActive = active, conditionCode = code, message = msg)
                }
                "school_commute_card" -> {
                    val run = dataElement["run"]?.jsonPrimitive?.contentOrNull ?: error("Missing run")
                    val date = dataElement["date"]?.jsonPrimitive?.contentOrNull ?: error("Missing date")
                    val window = dataElement["window"]?.jsonPrimitive?.contentOrNull ?: error("Missing window")
                    val verdict = dataElement["verdict"]?.jsonPrimitive?.contentOrNull ?: error("Missing verdict")
                    val reasons = dataElement["reasons"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val tips = dataElement["tips"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    WidgetUi.SchoolCommute(
                        type = type,
                        run = run,
                        date = date,
                        window = window,
                        verdict = verdict,
                        reasons = reasons,
                        tips = tips
                    )
                }
                "rain_alert_card" -> {
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: error("Missing message")
                    val peakProb = dataElement["peakProbabilityPercent"]?.jsonPrimitive?.intOrNull ?: error("Missing peakProbabilityPercent")
                    val peakHourLabel = dataElement["peakHourLabel"]?.jsonPrimitive?.contentOrNull
                    WidgetUi.RainAlert(
                        type = type,
                        message = message,
                        peakProbabilityPercent = peakProb,
                        peakHourLabel = peakHourLabel
                    )
                }
                "severe_weather_card" -> {
                    val message = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: error("Missing message")
                    val alertsList = dataElement["alerts"]?.jsonArray?.mapNotNull { item ->
                        if (item is JsonObject) {
                            val aType = item["type"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                            val startsAt = item["startsAt"]?.jsonPrimitive?.contentOrNull ?: ""
                            val msg = item["message"]?.jsonPrimitive?.contentOrNull ?: ""
                            WidgetUi.SevereAlertItem(aType, startsAt, msg)
                        } else null
                    } ?: emptyList()
                    WidgetUi.SevereWeather(
                        type = type,
                        alerts = alertsList,
                        message = message
                    )
                }
                "wave_height_card" -> {
                    val isCoastal = dataElement["isCoastal"]?.jsonPrimitive?.booleanOrNull ?: true
                    val height = dataElement["waveHeightMeters"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val period = dataElement["wavePeriodSeconds"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val swell = dataElement["swellHeightMeters"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val category = dataElement["category"]?.jsonPrimitive?.contentOrNull ?: "Calm"
                    val surfRating = dataElement["surfRating"]?.jsonPrimitive?.contentOrNull ?: "Flat"
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Wave height: $height m"
                    WidgetUi.WaveHeight(
                        type = type,
                        isCoastal = isCoastal,
                        waveHeightMeters = height,
                        wavePeriodSeconds = period,
                        swellHeightMeters = swell,
                        category = category,
                        surfRating = surfRating,
                        message = msg
                    )
                }
                "sea_temp_card" -> {
                    val temp = dataElement["waterTempCelsius"]?.jsonPrimitive?.doubleOrNull ?: 24.0
                    val isEstimated = dataElement["isEstimated"]?.jsonPrimitive?.booleanOrNull ?: false
                    val comfort = dataElement["comfort"]?.jsonPrimitive?.contentOrNull ?: "Comfortable"
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Water temp: $temp°C"
                    WidgetUi.SeaTemp(
                        type = type,
                        waterTempCelsius = temp,
                        isEstimated = isEstimated,
                        comfort = comfort,
                        message = msg
                    )
                }
                "tide_card" -> {
                    val date = dataElement["date"]?.jsonPrimitive?.contentOrNull ?: ""
                    val nextTideState = dataElement["nextTideState"]?.jsonPrimitive?.contentOrNull ?: "High Tide"
                    val nextTideTime = dataElement["nextTideTime"]?.jsonPrimitive?.contentOrNull ?: ""
                    val secondaryTideState = dataElement["secondaryTideState"]?.jsonPrimitive?.contentOrNull ?: "Low Tide"
                    val secondaryTideTime = dataElement["secondaryTideTime"]?.jsonPrimitive?.contentOrNull ?: ""
                    val tideSummary = dataElement["tideSummary"]?.jsonPrimitive?.contentOrNull ?: "$nextTideState around $nextTideTime"
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: tideSummary
                    WidgetUi.Tide(
                        type = type,
                        date = date,
                        nextTideState = nextTideState,
                        nextTideTime = nextTideTime,
                        secondaryTideState = secondaryTideState,
                        secondaryTideTime = secondaryTideTime,
                        tideSummary = tideSummary,
                        message = msg
                    )
                }
                "saved_destinations_card" -> {
                    val destArray = dataElement["destinations"]?.jsonArray
                    val items = destArray?.mapNotNull { el ->
                        if (el is JsonObject) {
                            val id = el["id"]?.jsonPrimitive?.contentOrNull ?: ""
                            val name = el["name"]?.jsonPrimitive?.contentOrNull ?: ""
                            val country = el["country"]?.jsonPrimitive?.contentOrNull ?: ""
                            val lat = el["lat"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val lon = el["lon"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val temp = el["tempCelsius"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val condition = el["condition"]?.jsonPrimitive?.contentOrNull ?: "Clear"
                            val code = el["weatherCode"]?.jsonPrimitive?.intOrNull ?: 0
                            val tip = el["packingTip"]?.jsonPrimitive?.contentOrNull ?: ""
                            WidgetUi.DestinationItem(id, name, country, lat, lon, temp, condition, code, tip)
                        } else null
                    } ?: emptyList()
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "Saved destinations"
                    WidgetUi.SavedDestinations(type = type, destinations = items, message = msg)
                }
                "flight_alert_card" -> {
                    val hasRisk = dataElement["hasDisruptionRisk"]?.jsonPrimitive?.booleanOrNull ?: false
                    val severity = dataElement["severity"]?.jsonPrimitive?.contentOrNull ?: "Normal"
                    val flightStatus = dataElement["flightStatus"]?.jsonPrimitive?.contentOrNull ?: "On Schedule"
                    val reasons = dataElement["reasons"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "No flight disruptions reported"
                    WidgetUi.FlightAlert(
                        type = type,
                        hasDisruptionRisk = hasRisk,
                        severity = severity,
                        flightStatus = flightStatus,
                        reasons = reasons,
                        message = msg
                    )
                }
                "packing_suggestion_card" -> {
                    val dest = dataElement["destination"]?.jsonPrimitive?.contentOrNull ?: ""
                    val headline = dataElement["headline"]?.jsonPrimitive?.contentOrNull ?: ""
                    val items = dataElement["items"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val tips = dataElement["tips"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val tempRange = dataElement["tempRange"]?.jsonPrimitive?.contentOrNull ?: ""
                    val rainChance = dataElement["rainChancePercent"]?.jsonPrimitive?.intOrNull ?: 0
                    val uvMax = dataElement["uvIndexMax"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: headline
                    WidgetUi.PackingSuggestion(
                        type = type,
                        destination = dest,
                        headline = headline,
                        items = items,
                        tips = tips,
                        tempRange = tempRange,
                        rainChancePercent = rainChance,
                        uvIndexMax = uvMax,
                        message = msg
                    )
                }
                // ── Gardener ──────────────────────────────────────────────────
                "soil_moisture_card" -> {
                    val level = dataElement["level"]?.jsonPrimitive?.contentOrNull ?: "Normal"
                    val precip = dataElement["precip7dMm"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val humidity = dataElement["humidityPercent"]?.jsonPrimitive?.intOrNull ?: 50
                    val advice = dataElement["advice"]?.jsonPrimitive?.contentOrNull ?: ""
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: advice
                    WidgetUi.SoilMoisture(type = type, level = level, precip7dMm = precip,
                        humidityPercent = humidity, advice = advice, message = msg)
                }
                "rainfall_prediction_card" -> {
                    val totalMm = dataElement["totalMm"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val maxProb = dataElement["maxProbabilityPercent"]?.jsonPrimitive?.intOrNull ?: 0
                    val rainyDays = dataElement["rainyDayCount"]?.jsonPrimitive?.intOrNull ?: 0
                    val forecastDays = dataElement["forecastDays"]?.jsonPrimitive?.intOrNull ?: 7
                    val outlook = dataElement["outlook"]?.jsonPrimitive?.contentOrNull ?: ""
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: outlook
                    WidgetUi.RainfallPrediction(type = type, totalMm = totalMm,
                        maxProbabilityPercent = maxProb, rainyDayCount = rainyDays,
                        forecastDays = forecastDays, outlook = outlook, message = msg)
                }
                "frost_alert_card" -> {
                    val active = dataElement["alertActive"]?.jsonPrimitive?.booleanOrNull ?: false
                    val alertLevel = dataElement["alertLevel"]?.jsonPrimitive?.contentOrNull ?: "Clear"
                    val frostDays = dataElement["frostRiskDayCount"]?.jsonPrimitive?.intOrNull ?: 0
                    val hardFrostDays = dataElement["hardFrostDayCount"]?.jsonPrimitive?.intOrNull ?: 0
                    val advice = dataElement["advice"]?.jsonPrimitive?.contentOrNull ?: ""
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: advice
                    WidgetUi.FrostAlert(type = type, alertActive = active, alertLevel = alertLevel,
                        frostRiskDayCount = frostDays, hardFrostDayCount = hardFrostDays,
                        advice = advice, message = msg)
                }
                "planting_guidance_card" -> {
                    val hasFrost = dataElement["hasFrostRisk"]?.jsonPrimitive?.booleanOrNull ?: false
                    val avgMax = dataElement["avgMaxTempC"]?.jsonPrimitive?.doubleOrNull ?: 22.0
                    val avgMin = dataElement["avgMinTempC"]?.jsonPrimitive?.doubleOrNull ?: 12.0
                    val totalRain = dataElement["totalRainMm7d"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    val guidance = dataElement["guidance"]?.jsonPrimitive?.contentOrNull ?: ""
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: guidance
                    WidgetUi.PlantingGuidance(type = type, hasFrostRisk = hasFrost,
                        avgMaxTempC = avgMax, avgMinTempC = avgMin, totalRainMm7d = totalRain,
                        guidance = guidance, message = msg)
                }
                // ── Event Planner ─────────────────────────────────────────────
                "extended_forecast_card" -> {
                    val daysArray = dataElement["days"]?.jsonArray
                    val dayItems = daysArray?.mapNotNull { el ->
                        if (el is JsonObject) {
                            val date = el["date"]?.jsonPrimitive?.contentOrNull ?: ""
                            val condition = el["condition"]?.jsonPrimitive?.contentOrNull ?: "Variable"
                            val maxT = el["maxTempC"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val minT = el["minTempC"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                            val prob = el["precipProbPercent"]?.jsonPrimitive?.intOrNull ?: 0
                            WidgetUi.DayForecastItem(date, condition, maxT, minT, prob)
                        } else null
                    } ?: emptyList()
                    val count = dataElement["forecastDayCount"]?.jsonPrimitive?.intOrNull ?: dayItems.size
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: "${count}-day forecast"
                    WidgetUi.ExtendedForecast(type = type, days = dayItems, forecastDayCount = count, message = msg)
                }
                "rain_probability_card" -> {
                    val peak = dataElement["peakEventHourProbPercent"]?.jsonPrimitive?.intOrNull ?: 0
                    val avg = dataElement["avgEventHourProbPercent"]?.jsonPrimitive?.intOrNull ?: 0
                    val daily3 = dataElement["daily3MaxProbPercent"]?.jsonPrimitive?.intOrNull ?: 0
                    val outlook = dataElement["outlook"]?.jsonPrimitive?.contentOrNull ?: ""
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: outlook
                    WidgetUi.RainProbability(type = type, peakEventHourProbPercent = peak,
                        avgEventHourProbPercent = avg, daily3MaxProbPercent = daily3,
                        outlook = outlook, message = msg)
                }
                "comfort_index_card" -> {
                    val score = dataElement["comfortIndex"]?.jsonPrimitive?.intOrNull ?: 0
                    val rating = dataElement["rating"]?.jsonPrimitive?.contentOrNull ?: "Fair"
                    val temp = dataElement["tempCelsius"]?.jsonPrimitive?.doubleOrNull ?: 22.0
                    val apparent = dataElement["apparentTempCelsius"]?.jsonPrimitive?.doubleOrNull ?: temp
                    val humidity = dataElement["humidityPercent"]?.jsonPrimitive?.intOrNull ?: 50
                    val uv = dataElement["uvIndex"]?.jsonPrimitive?.doubleOrNull ?: 3.0
                    val advice = dataElement["advice"]?.jsonPrimitive?.contentOrNull ?: ""
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: advice
                    WidgetUi.ComfortIndex(type = type, comfortIndex = score, rating = rating,
                        tempCelsius = temp, apparentTempCelsius = apparent, humidityPercent = humidity,
                        uvIndex = uv, advice = advice, message = msg)
                }
                else -> {
                    WidgetUi.Unsupported(type = type)
                }
            }
        } catch (_: Exception) {
            WidgetUi.StatusOnly(
                type = type,
                status = "error",
                message = "Temporarily unavailable"
            )
        }
    }
}
