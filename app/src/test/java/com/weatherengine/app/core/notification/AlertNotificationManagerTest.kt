package com.weatherengine.app.core.notification

import com.weatherengine.app.core.WidgetUi
import org.junit.Assert.assertNotNull
import org.junit.Test

class AlertNotificationManagerTest {

    @Test
    fun testAlertNotificationManager_objectExistsAndLoads() {
        assertNotNull(AlertNotificationManager)
    }

    @Test
    fun testAlertTypes_canBeCategorized() {
        val heat = WidgetUi.HeatAlert(
            alertActive = true,
            severity = "High",
            temperatureCelsius = 39.0,
            message = "Extreme heat advisory"
        )
        val severe = WidgetUi.SevereWeather(
            alerts = listOf(WidgetUi.SevereAlertItem("Thunderstorm", "14:00", "High wind and hail")),
            message = "Stay indoors"
        )

        val widgets = listOf(heat, severe)
        val activeCount = widgets.count {
            when (it) {
                is WidgetUi.HeatAlert -> it.alertActive
                is WidgetUi.SevereWeather -> true
                else -> false
            }
        }
        org.junit.Assert.assertEquals(2, activeCount)
    }

    @Test
    fun testPredictiveWeatherAlertDto_creationAndFields() {
        val alert = com.weatherengine.app.data.model.PredictiveWeatherAlertDto(
            targetHour = 17,
            targetTimeFormatted = "5:00 PM",
            predictedLatitude = 28.6139,
            predictedLongitude = 77.2090,
            alertType = "RAIN",
            headline = "🌧️ Rain Forecast for 5:00 PM",
            message = "Heavy rain expected on your commute. Carry an umbrella!",
            severity = "CRITICAL"
        )

        org.junit.Assert.assertEquals(17, alert.targetHour)
        org.junit.Assert.assertEquals("RAIN", alert.alertType)
        org.junit.Assert.assertEquals("CRITICAL", alert.severity)
    }
}

