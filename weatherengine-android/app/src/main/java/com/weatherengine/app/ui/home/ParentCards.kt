package com.weatherengine.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.core.WidgetUi
import com.weatherengine.app.ui.theme.AmberWarning
import com.weatherengine.app.ui.theme.ErrorRed
import com.weatherengine.app.ui.theme.SuccessGreen

@Composable
fun SchoolCommuteCard(
    commute: WidgetUi.SchoolCommute,
    modifier: Modifier = Modifier
) {
    val (verdictColor, verdictIcon, verdictText) = when (commute.verdict.lowercase()) {
        "caution" -> Triple(ErrorRed, Icons.Default.Warning, "Caution")
        "prepare" -> Triple(AmberWarning, Icons.Default.Info, "Prepare")
        else -> Triple(SuccessGreen, Icons.Default.CheckCircle, "Good")
    }

    val runLabel = if (commute.run.equals("morning", ignoreCase = true)) "Morning School Run" else "Afternoon School Run"
    val a11ySummary = "$runLabel verdict is $verdictText. Window: ${commute.window} on ${commute.date}."

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11ySummary },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, verdictColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = runLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${commute.date} · ${commute.window}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Verdict with Icon + Text + Colour (never colour alone)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = verdictColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, verdictColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = verdictIcon,
                            contentDescription = verdictText,
                            tint = verdictColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = verdictText,
                            fontWeight = FontWeight.Bold,
                            color = verdictColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (commute.reasons.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Conditions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                commute.reasons.forEach { reason ->
                    Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (commute.tips.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Tips", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                commute.tips.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Assumes Monday–Friday school schedule",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RainAlertCard(
    rain: WidgetUi.RainAlert,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Rain Lookahead: ${rain.message}. Peak chance ${rain.peakProbabilityPercent}%." },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Rain Lookahead (Next 6 Hours)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = rain.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            val detail = if (!rain.peakHourLabel.isNullOrBlank()) {
                "Peak probability: ${rain.peakProbabilityPercent}% around ${rain.peakHourLabel}"
            } else {
                "Peak probability: ${rain.peakProbabilityPercent}%"
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SevereWeatherCard(
    severe: WidgetUi.SevereWeather,
    modifier: Modifier = Modifier
) {
    val hasAlerts = severe.alerts.isNotEmpty()
    val borderColor = if (hasAlerts) ErrorRed.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Severe Weather: ${severe.message}." },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasAlerts) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (hasAlerts) ErrorRed else SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Severe Weather (Next 24 Hours)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = severe.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasAlerts) FontWeight.Bold else FontWeight.Normal
            )

            if (hasAlerts) {
                Spacer(Modifier.height(8.dp))
                severe.alerts.forEach { alert ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ErrorRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = alert.startsAt,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${alert.type.replace('_', ' ').replaceFirstChar { it.uppercase() }}: ${alert.message}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "App estimate, not an official IMD warning",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatusOnlyCard(
    widget: WidgetUi.StatusOnly,
    modifier: Modifier = Modifier
) {
    val cardTitle = when (widget.type) {
        "school_commute_card" -> "School Commute"
        "rain_alert_card" -> "Rain Lookahead"
        "severe_weather_card" -> "Severe Weather"
        else -> widget.type.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$cardTitle: ${widget.message}." },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = cardTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = widget.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
