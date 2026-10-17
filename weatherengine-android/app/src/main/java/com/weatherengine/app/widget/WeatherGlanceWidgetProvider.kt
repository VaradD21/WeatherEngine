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
import com.weatherengine.app.core.forecast.ForecastUi

class WeatherGlanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, null, null)
        }
    }

    companion object {
        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forecast: ForecastUi? = null,
            locationLabel: String? = null
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather_glance)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.glance_widget_root, pendingIntent)

            if (forecast != null) {
                val current = forecast.current
                views.setTextViewText(R.id.glance_location, locationLabel ?: "WeatherEngine")
                views.setTextViewText(R.id.glance_temperature, current.temperatureFormatted)
                views.setTextViewText(R.id.glance_condition, current.condition)

                val badgeText = when {
                    current.aqi != null -> "AQI ${current.aqi}"
                    current.uvIndex != null -> "UV ${current.uvIndex}"
                    current.windSpeedKmh != null -> "${current.windSpeedKmh.toInt()} km/h"
                    else -> current.windFormatted
                }
                views.setTextViewText(R.id.glance_badge_text, badgeText)
            } else {
                views.setTextViewText(R.id.glance_location, locationLabel ?: "WeatherEngine")
                views.setTextViewText(R.id.glance_condition, "Tap to sync")
                views.setTextViewText(R.id.glance_temperature, "--°")
                views.setTextViewText(R.id.glance_badge_text, "WeatherEngine")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, forecast: ForecastUi? = null, locationLabel: String? = null) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, WeatherGlanceWidgetProvider::class.java)
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
                val provider = ComponentName(context, WeatherGlanceWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val pinnedSuccessIntent = Intent(context, WeatherGlanceWidgetProvider::class.java)
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
