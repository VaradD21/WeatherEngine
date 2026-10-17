package com.weatherengine.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import com.weatherengine.app.MainActivity
import com.weatherengine.app.R
import com.weatherengine.app.core.forecast.ForecastUi

class AqiAlertWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_aqi_alert)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                2,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.aqi_widget_root, pendingIntent)

            views.setTextViewText(R.id.aqi_widget_location, locationLabel ?: "WeatherEngine")

            if (forecast != null) {
                val current = forecast.current
                val aqi = current.aqi
                val category = current.aqiCategory ?: "Moderate"

                if (aqi != null) {
                    views.setTextViewText(R.id.aqi_widget_value, aqi.toString())
                    val colorHex = when {
                        aqi <= 50 -> "#4ADE80" // Green
                        aqi <= 100 -> "#FACC15" // Yellow
                        aqi <= 150 -> "#FB923C" // Orange
                        else -> "#F87171" // Red
                    }
                    views.setTextColor(R.id.aqi_widget_value, Color.parseColor(colorHex))
                } else {
                    views.setTextViewText(R.id.aqi_widget_value, "--")
                }

                views.setTextViewText(R.id.aqi_widget_category, "AQI · $category")
                val uv = current.uvIndex?.let { "$it" } ?: "--"
                val pm = current.pm25?.let { "%.1f".format(it) } ?: "--"
                views.setTextViewText(R.id.aqi_widget_subtext, "UV: $uv · PM2.5: $pm µg/m³")

                val alertMsg = when {
                    aqi != null && aqi > 150 -> "⚠️ Unhealthy Air — Limit outdoor exposure"
                    forecast.heat is com.weatherengine.app.persona.HeatStatus.Alert -> "🔥 Extreme Heat Alert: ${(forecast.heat as com.weatherengine.app.persona.HeatStatus.Alert).message}"
                    forecast.stormFog.alertActive -> "⛈️ Commute Alert: ${forecast.stormFog.message}"
                    else -> "Shield Active · Safe breathing conditions"
                }
                views.setTextViewText(R.id.aqi_widget_alert, alertMsg)
            } else {
                views.setTextViewText(R.id.aqi_widget_value, "--")
                views.setTextViewText(R.id.aqi_widget_category, "Air & Health Shield")
                views.setTextViewText(R.id.aqi_widget_subtext, "Tap to open and sync metrics")
                views.setTextViewText(R.id.aqi_widget_alert, "Live shield ready")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, forecast: ForecastUi? = null, locationLabel: String? = null) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, AqiAlertWidgetProvider::class.java)
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
                val provider = ComponentName(context, AqiAlertWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val pinnedSuccessIntent = Intent(context, AqiAlertWidgetProvider::class.java)
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
