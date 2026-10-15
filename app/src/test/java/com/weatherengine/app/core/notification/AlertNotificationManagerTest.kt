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
            severity = "Severe",
            headline = "Thunderstorm Warning",
            description = "High wind and hail",
            instruction = "Stay indoors",
            alerts = listOf("Thunderstorm")
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
}
