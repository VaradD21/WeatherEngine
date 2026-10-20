package com.weatherengine.app.widget

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Internal unexported receiver for custom weather widget refresh events.
 * Keeps com.weatherengine.app.ACTION_UPDATE_WEATHER scoped to the application process,
 * preventing spoofed broadcasts from malicious third-party apps on the device.
 */
class WeatherUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_UPDATE_WEATHER) {
            WeatherAppWidgetProvider.updateAllWidgets(context)
            WeatherGlanceWidgetProvider.updateAllWidgets(context)
            AqiAlertWidgetProvider.updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_UPDATE_WEATHER = "com.weatherengine.app.ACTION_UPDATE_WEATHER"

        fun createUpdateIntent(context: Context): Intent =
            Intent(context, WeatherUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE_WEATHER
                setPackage(context.packageName)
            }

        fun createUpdatePendingIntent(context: Context, requestCode: Int = 100): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                createUpdateIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
