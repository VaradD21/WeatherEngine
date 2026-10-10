package com.weatherengine.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.weatherengine.app.data.model.HealthProfileDto
import com.weatherengine.app.data.model.WeatherBundleDto
import kotlin.math.roundToInt

object HealthAlertNotificationManager {

    private const val CHANNEL_ID = "weather_health_channel"
    private const val CHANNEL_NAME = "Health & Weather Alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom notifications for air quality, UV, pollen, and humidity limits"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun evaluateAndNotify(
        context: Context,
        bundle: WeatherBundleDto,
        profile: HealthProfileDto
    ) {
        if (!profile.alertsEnabled) return
        ensureChannel(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val aqi = bundle.airQuality?.current?.usAqi
        if (aqi != null && aqi >= profile.aqiThreshold) {
            val title = if (profile.hasAsthma) "Asthma Alert: High AQI ($aqi)" else "Air Quality Alert: AQI $aqi"
            val text = "Current AQI ($aqi) exceeds your alert threshold (${profile.aqiThreshold}). Limit outdoor exposure."
            showNotification(context, nm, 1001, title, text)
        }

        val uv = bundle.forecast.hourly?.uvIndex?.firstOrNull { it != null } ?: 0.0
        if (uv >= profile.uvThreshold) {
            val title = if (profile.hasSkinSensitivity) "Skin Alert: High UV Index ($uv)" else "UV Radiation Alert: $uv"
            val text = "UV level exceeds your threshold of ${profile.uvThreshold}. Apply sunscreen and seek shade."
            showNotification(context, nm, 1002, title, text)
        }

        val humidity = bundle.forecast.current?.relativeHumidity2m
        if (humidity != null && humidity >= profile.humidityThreshold) {
            val title = "High Humidity Alert: $humidity%"
            val text = "Humidity is $humidity% (limit: ${profile.humidityThreshold}%). May aggravate respiratory symptoms."
            showNotification(context, nm, 1003, title, text)
        }
    }

    private fun showNotification(
        context: Context,
        nm: NotificationManager,
        notificationId: Int,
        title: String,
        content: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            nm.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not yet granted by user
        }
    }
}
