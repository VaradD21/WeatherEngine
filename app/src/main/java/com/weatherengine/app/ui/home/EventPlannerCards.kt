package com.weatherengine.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherengine.app.core.WidgetUi
import com.weatherengine.app.ui.theme.AmberWarning
import com.weatherengine.app.ui.theme.ErrorRed
import com.weatherengine.app.ui.theme.GlassCard
import com.weatherengine.app.ui.theme.SuccessGreen

// ── Extended Forecast ─────────────────────────────────────────────────────────
@Composable
fun ExtendedForecastCard(forecast: WidgetUi.ExtendedForecast, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Extended forecast: ${forecast.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${forecast.forecastDayCount}-Day Forecast",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            forecast.days.take(7).forEach { day ->
                val probColor = when {
                    day.precipProbPercent >= 60 -> ErrorRed
                    day.precipProbPercent >= 30 -> AmberWarning
                    else -> SuccessGreen
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.date.takeLast(5),   // MM-DD
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp)
                    )
                    Text(
                        text = day.condition,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${day.minTempC.toInt()}° / ${day.maxTempC.toInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        text = "${day.precipProbPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = probColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

// ── Rain Probability ──────────────────────────────────────────────────────────
@Composable
fun RainProbabilityCard(rain: WidgetUi.RainProbability, modifier: Modifier = Modifier) {
    val riskColor = when {
        rain.peakEventHourProbPercent >= 70 -> ErrorRed
        rain.peakEventHourProbPercent >= 50 -> AmberWarning
        else -> SuccessGreen
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Event rain probability: ${rain.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Umbrella,
                    contentDescription = null,
                    tint = riskColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Event Rain Probability",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        text = "${rain.peakEventHourProbPercent}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                    Text(
                        text = "Peak (3–7 PM)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "${rain.daily3MaxProbPercent}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "3-day max",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rain.outlook,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Comfort Index ─────────────────────────────────────────────────────────────
@Composable
fun ComfortIndexCard(comfort: WidgetUi.ComfortIndex, modifier: Modifier = Modifier) {
    val comfortColor = when (comfort.rating) {
        "Excellent" -> SuccessGreen
        "Good" -> MaterialTheme.colorScheme.primary
        "Fair" -> AmberWarning
        else -> ErrorRed
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Outdoor comfort: ${comfort.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = comfortColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Outdoor Comfort",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = comfort.rating,
                        style = MaterialTheme.typography.labelSmall,
                        color = comfortColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${comfort.comfortIndex}/100",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = comfortColor
                )
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Thermostat, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${comfort.tempCelsius}°C", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "${comfort.humidityPercent}% humidity · UV ${comfort.uvIndex}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comfort.advice,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
