package com.weatherengine.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.weatherengine.app.MainActivity
import com.weatherengine.app.core.WidgetUi
import java.util.concurrent.ConcurrentHashMap

object AlertNotificationManager {

    private const val CHANNEL_ID = "weather_alerts_channel"
    private const val CHANNEL_NAME = "Weather Alerts & Advisories"
    private const val CHANNEL_DESC = "Urgent severe weather warnings, heat advisories, and outdoor condition alerts"

    // Dedup cache: key -> timestamp (millis)
    private val notifiedAlerts = ConcurrentHashMap<String, Long>()
    private const val DEDUP_COOLDOWN_MS = 2 * 60 * 60 * 1000L // 2 hours

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifyAlerts(
        context: Context,
        widgets: List<WidgetUi>,
        notificationsEnabled: Boolean = true
    ) {
        if (!notificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        createNotificationChannel(context)

        for (widget in widgets) {
            when (widget) {
                is WidgetUi.SevereWeather -> {
                    val firstAlert = widget.alerts.firstOrNull()
                    val alertType = firstAlert?.type ?: "Severe Weather"
                    postNotificationIfFresh(
                        context = context,
                        key = "severe_${alertType}_${widget.message.hashCode()}",
                        id = 1001,
                        title = "⚠️ Weather Alert: $alertType",
                        message = firstAlert?.message ?: widget.message
                    )
                }
                is WidgetUi.HeatAlert -> {
                    if (widget.alertActive) {
                        postNotificationIfFresh(
                            context = context,
                            key = "heat_${widget.temperatureCelsius.toInt()}",
                            id = 1002,
                            title = "🔥 Heat Alert: ${widget.severity}",
                            message = widget.message
                        )
                    }
                }
                is WidgetUi.StormFog -> {
                    if (widget.alertActive) {
                        postNotificationIfFresh(
                            context = context,
                            key = "storm_fog_${widget.conditionCode}",
                            id = 1003,
                            title = "🌫️ Commute Weather Advisory",
                            message = widget.message
                        )
                    }
                }
                is WidgetUi.FrostAlert -> {
                    if (widget.alertActive) {
                        postNotificationIfFresh(
                            context = context,
                            key = "frost_${widget.alertLevel}_${widget.frostRiskDayCount}",
                            id = 1004,
                            title = "❄️ Frost Warning (${widget.alertLevel})",
                            message = widget.message
                        )
                    }
                }
                is WidgetUi.RainAlert -> {
                    if (widget.peakProbabilityPercent >= 70 && widget.peakHourLabel != null) {
                        postNotificationIfFresh(
                            context = context,
                            key = "rain_${widget.peakHourLabel}",
                            id = 1005,
                            title = "🌧️ Heavy Rain Approaching",
                            message = widget.message
                        )
                    }
                }
                is WidgetUi.FlightAlert -> {
                    if (widget.hasDisruptionRisk) {
                        postNotificationIfFresh(
                            context = context,
                            key = "flight_${widget.flightStatus}_${widget.severity}",
                            id = 1006,
                            title = "✈️ Flight Weather Alert: ${widget.flightStatus}",
                            message = widget.message
                        )
                    }
                }
                is WidgetUi.Aqi -> {
                    if (widget.aqi >= 150) {
                        postNotificationIfFresh(
                            context = context,
                            key = "aqi_${widget.aqi}",
                            id = 1007,
                            title = "😷 Air Quality Alert: ${widget.category}",
                            message = "AQI is ${widget.aqi}. Sensitive groups should limit prolonged outdoor exposure."
                        )
                    }
                }
                else -> {}
            }
        }
    }

    private fun postNotificationIfFresh(
        context: Context,
        key: String,
        id: Int,
        title: String,
        message: String
    ) {
        val now = System.currentTimeMillis()
        val lastSent = notifiedAlerts[key]
        if (lastSent != null && (now - lastSent) < DEDUP_COOLDOWN_MS) {
            return
        }

        notifiedAlerts[key] = now

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
