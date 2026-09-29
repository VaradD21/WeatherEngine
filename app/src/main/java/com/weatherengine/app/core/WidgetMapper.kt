package com.weatherengine.app.core

import com.weatherengine.app.core.models.WidgetUi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object WidgetMapper {

    fun mapWidget(type: String, dataElement: JsonElement?): WidgetUi {
        if (dataElement !is JsonObject) {
            return WidgetUi.StatusOnly(type = type, status = "error", message = "Invalid data format")
        }

        return try {
            // Check if widget returns a status-only state ("error", "unavailable", "mocked")
            val status = dataElement["status"]?.jsonPrimitive?.contentOrNull
            if (status == "error" || status == "unavailable" || status == "mocked") {
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
                "sunrise_sunset_card" -> {
                    val sunrise = dataElement["sunrise"]?.jsonPrimitive?.contentOrNull ?: error("Missing sunrise")
                    val sunset = dataElement["sunset"]?.jsonPrimitive?.contentOrNull ?: error("Missing sunset")
                    WidgetUi.SunriseSunset(type = type, sunrise = sunrise, sunset = sunset)
                }
                "wind_speed_card" -> {
                    val speed = dataElement["speedMetersPerSecond"]?.jsonPrimitive?.doubleOrNull ?: error("Missing speedMetersPerSecond")
                    WidgetUi.Wind(type = type, speedMetersPerSecond = speed)
                }
                "heat_alert_card" -> {
                    val active = dataElement["alertActive"]?.jsonPrimitive?.booleanOrNull ?: error("Missing alertActive")
                    val temp = dataElement["temperatureCelsius"]?.jsonPrimitive?.doubleOrNull ?: error("Missing temperatureCelsius")
                    val msg = dataElement["message"]?.jsonPrimitive?.contentOrNull ?: error("Missing message")
                    WidgetUi.HeatAlert(type = type, alertActive = active, temperatureCelsius = temp, message = msg)
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

    private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
        get() = try { content } catch (_: Exception) { null }
}
