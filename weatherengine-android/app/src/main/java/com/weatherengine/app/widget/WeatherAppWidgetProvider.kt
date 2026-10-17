package com.weatherengine.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.weatherengine.app.MainActivity
import com.weatherengine.app.R
import com.weatherengine.app.core.forecast.CurrentWeatherUi
import com.weatherengine.app.core.forecast.ForecastUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, null, null)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WEATHER) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WeatherAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateWidgetView(context, appWidgetManager, id, null, null)
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WEATHER = "com.weatherengine.app.ACTION_UPDATE_WEATHER"

        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forecast: ForecastUi? = null,
            locationLabel: String? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            if (forecast != null) {
                val current = forecast.current
                val loc = locationLabel ?: "WeatherEngine"
                views.setTextViewText(R.id.widget_location, loc)
                views.setTextViewText(R.id.widget_temperature, current.temperatureFormatted)
                views.setTextViewText(R.id.widget_condition, current.condition)

                val dayRange = forecast.days.firstOrNull()
                val rangeText = if (dayRange != null) {
                    "H:${dayRange.maxTempFormatted}  L:${dayRange.minTempFormatted} · Feels ${current.apparentTemperatureFormatted}"
                } else {
                    "Feels like ${current.apparentTemperatureFormatted}"
                }
                views.setTextViewText(R.id.widget_temp_range, rangeText)

                val badgeText = when {
                    current.aqi != null -> "AQI ${current.aqi} · ${current.aqiCategory}"
                    current.uvIndex != null -> "UV ${current.uvIndex}"
                    current.windSpeedKmh != null -> "Wind ${current.windSpeedKmh.toInt()} km/h"
                    else -> current.windFormatted
                }
                views.setTextViewText(R.id.widget_badge_text, badgeText)
            } else {
                views.setTextViewText(R.id.widget_location, locationLabel ?: "WeatherEngine")
                views.setTextViewText(R.id.widget_condition, "Tap to open app")
            }

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            views.setTextViewText(R.id.widget_updated_time, timeFormat.format(Date()))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, forecast: ForecastUi? = null, locationLabel: String? = null) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, WeatherAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (allWidgetIds.isNotEmpty()) {
                    for (id in allWidgetIds) {
                        updateWidgetView(context, appWidgetManager, id, forecast, locationLabel)
                    }
                }
            } catch (_: Throwable) {}
        }

        fun pinToHomeScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                val provider = ComponentName(context, WeatherAppWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val pinnedSuccessIntent = Intent(context, WeatherAppWidgetProvider::class.java)
                    val successCallback = PendingIntent.getBroadcast(
                        context,
                        0,
                        pinnedSuccessIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    return appWidgetManager.requestPinAppWidget(provider, null, successCallback)
                }
            }
            return false
        }
    }
}
