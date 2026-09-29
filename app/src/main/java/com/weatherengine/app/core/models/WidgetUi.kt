package com.weatherengine.app.core.models

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
