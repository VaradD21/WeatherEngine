package com.weatherengine.backend.weather

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WidgetBuilderTest {

    private val widgetBuilder = WidgetBuilder()

    @Test
    fun `buildWidget maps aqi, humidity, and heat_alert cards accurately`() {
        val bundle = WeatherBundleDto(
            forecast = OpenMeteoForecastResponse(
                current = OpenMeteoCurrentWeather(
                    temperature2m = 31.0,
                    apparentTemperature = 35.0,
                    relativeHumidity2m = 72,
                    windSpeed10m = 18.0,
                    weatherCode = 0
                ),
                hourly = OpenMeteoHourlyForecast(
                    apparentTemperature = listOf(35.0, 36.0),
                    uvIndex = listOf(7.5),
                    visibility = listOf(10000.0)
                )
            ),
            airQuality = OpenMeteoAirQualityResponse(
                current = OpenMeteoCurrentAirQuality(
                    usAqi = 42,
                    pm25 = 9.5
                )
            )
        )

        val aqiWidget = widgetBuilder.buildWidget("aqi_card", bundle)
        assertEquals("aqi_card", aqiWidget.type)
        assertEquals(42, aqiWidget.data["aqi"])
        assertEquals("Good", aqiWidget.data["category"])

        val humidityWidget = widgetBuilder.buildWidget("humidity_card", bundle)
        assertEquals(72, humidityWidget.data["humidityPercent"])

        val heatWidget = widgetBuilder.buildWidget("heat_alert_card", bundle)
        assertEquals(true, heatWidget.data["alertActive"])
        assertEquals(35.0, heatWidget.data["temperatureCelsius"])
    }
}
