package com.weatherengine.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeatherAppWidgetProviderTest {

    @Test
    fun testProviderActionConstant() {
        assertEquals("com.weatherengine.app.ACTION_UPDATE_WEATHER", WeatherAppWidgetProvider.ACTION_UPDATE_WEATHER)
        assertEquals("com.weatherengine.app.ACTION_UPDATE_WEATHER", WeatherUpdateReceiver.ACTION_UPDATE_WEATHER)
        assertNotNull(WeatherAppWidgetProvider())
        assertNotNull(WeatherGlanceWidgetProvider())
        assertNotNull(AqiAlertWidgetProvider())
        assertNotNull(WeatherUpdateReceiver())
    }
}
