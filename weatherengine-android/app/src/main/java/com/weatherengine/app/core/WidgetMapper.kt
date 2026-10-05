package com.weatherengine.app.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed class WidgetUi(open val type: String) {
    data class Aqi(
        override val type: String = "aqi_card",
        val aqi: Int,
        val category: String
    ) : WidgetUi(type)

    data class Humidity(
        override val type: String = "humidity_card",
        val humidityPercent: Int
    ) : WidgetUi(type)

    data class Uv(
        override val type: String = "uv_index_card",
        val status: String?,
        val uvIndex: Double?,
        val message: String?
    ) : WidgetUi(type)

    data class SunriseSunset(
        override val type: String = "sunrise_sunset_card",
        val sunrise: String,
        val sunset: String
    ) : WidgetUi(type)

    data class Wind(
        override val type: String = "wind_speed_card",
        val speedMetersPerSecond: Double
    ) : WidgetUi(type)

    data class HeatAlert(
        override val type: String = "heat_alert_card",
        val alertActive: Boolean,
        val temperatureCelsius: Double,
        val message: String
    ) : WidgetUi(type)

    data class Traffic(
        override val type: String = "traffic_card",
        val status: String,
        val message: String
    ) : WidgetUi(type)

    data class Visibility(
        override val type: String = "visibility_card",
        val visibilityMeters: Int,
        val category: String
    ) : WidgetUi(type)

    data class StormFog(
        override val type: String = "storm_fog_alert_card",
        val alertActive: Boolean,
        val conditionCode: Int,
        val message: String
    ) : WidgetUi(type)

    data class StatusOnly(
        override val type: String,
        val status: String,
        val message: String
    ) : WidgetUi(type)

    data class Unsupported(
        override val type: String
    ) : WidgetUi(type)
}

object WidgetMapper {

    fun mapWidget(type: String, dataElement: JsonElement?): WidgetUi {
        if (dataElement !is JsonObject) {
            return WidgetUi.StatusOnly(type = type, status = "error", message = "Invalid data format")
        }

        return try {
            // Check if widget returns a status-only state ("error", "unavailable")
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
}

