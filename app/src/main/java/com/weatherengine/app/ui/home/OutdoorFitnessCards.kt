package com.weatherengine.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
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
fun SunriseSunsetCard(
    sun: WidgetUi.SunriseSunset,
    modifier: Modifier = Modifier
) {
    com.weatherengine.app.ui.theme.GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Sunrise at ${sun.sunrise}, sunset at ${sun.sunset}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sun & Daylight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sunrise", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sun.sunrise, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Sunset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sun.sunset, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (sun.dayLength.isNotBlank()) {
                    Column {
                        Text("Day Length", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(sun.dayLength, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (sun.message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = sun.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BestRunningHoursCard(
    running: WidgetUi.BestRunning,
    modifier: Modifier = Modifier
) {
    val statusColor = if (running.hasOptimalWindow) SuccessGreen else AmberWarning

    com.weatherengine.app.ui.theme.GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Running planner: ${running.message}" },
        shape = RoundedCornerShape(24.dp),
        borderBrush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(statusColor.copy(alpha = 0.5f), statusColor.copy(alpha = 0.15f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Best Running Window",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (running.hasOptimalWindow && running.window != null) {
                    Text(
                        text = running.window,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (running.score != null) {
                        Text(
                            text = "Score: ${running.score}/100 • ${running.reasons.joinToString(" • ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = running.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WindSpeedCard(
    wind: WidgetUi.Wind,
    modifier: Modifier = Modifier
) {
    com.weatherengine.app.ui.theme.GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Wind speed: ${wind.speedKmh} km/h, ${wind.category}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Wind & Workout Conditions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${wind.speedKmh} km/h — ${wind.category}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (wind.gustsKmh > wind.speedKmh) {
                    Text(
                        text = "Peak gusts up to ${wind.gustsKmh} km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (wind.message.isNotBlank()) {
                    Text(
                        text = wind.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HeatAlertCard(
    heat: WidgetUi.HeatAlert,
    modifier: Modifier = Modifier
) {
    val (statusColor, icon) = when (heat.severity.lowercase()) {
        "warning" -> Pair(ErrorRed, Icons.Default.Warning)
        "caution" -> Pair(AmberWarning, Icons.Default.Warning)
        else -> Pair(SuccessGreen, Icons.Default.CheckCircle)
    }

    com.weatherengine.app.ui.theme.GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Heat status: ${heat.message}" },
        shape = RoundedCornerShape(24.dp),
        borderBrush = androidx.compose.ui.graphics.Brush.linearGradient(
            listOf(statusColor.copy(alpha = 0.6f), statusColor.copy(alpha = 0.15f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Heat & Workout Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = heat.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (heat.alertActive) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "App estimate, not an official IMD warning",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
