package com.weatherengine.app.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.persona.BestRunningResult
import com.weatherengine.app.persona.DaylightState
import com.weatherengine.app.persona.PersonaRules
import kotlin.math.roundToInt

@Composable
fun CardShell(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    com.weatherengine.app.ui.theme.GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun AqiPersonaCard(forecast: ForecastUi) {
    val aqi = forecast.current.aqi
    val pm25 = forecast.current.pm25
    val category = forecast.current.aqiCategory

    CardShell("Air Quality (US AQI)") {
        if (aqi != null) {
            Column {
                Text(
                    text = "$aqi — $category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (pm25 != null) {
                    Text(
                        text = "PM2.5: $pm25 μg/m³",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text("Air quality data is currently unavailable", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun UvPersonaCard(forecast: ForecastUi) {
    val currentUv = forecast.current.uvIndex
    val maxUv = forecast.uvMaxToday
    val category = PersonaRules.uvCategory(currentUv)

    CardShell("UV Index") {
        if (currentUv != null) {
            Column {
                Text(
                    text = "${String.format("%.1f", currentUv)} — $category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (maxUv != null) {
                    Text(
                        text = "Today's Peak: ${String.format("%.1f", maxUv)} (${PersonaRules.uvCategory(maxUv)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text("UV index is currently unavailable", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun HumidityPersonaCard(forecast: ForecastUi) {
    val humidity = forecast.current.humidityPercent
    val comfort = PersonaRules.humidityComfort(humidity)

    CardShell("Humidity & Comfort") {
        if (humidity != null) {
            Text(
                text = "$humidity% — $comfort",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text("Humidity data is currently unavailable", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PollenPersonaCard(forecast: ForecastUi) {
    val pollen = forecast.pollen
    CardShell("Pollen Level") {
        if (pollen.isAvailable && pollen.highestValue != null) {
            Column {
                Text(
                    text = pollen.levelLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Main allergen: ${pollen.highestName} (${pollen.highestValue.roundToInt()} grains/m³)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Pollen data is not available for this region",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SunPersonaCard(forecast: ForecastUi) {
    CardShell("Sun & Daylight") {
        when (val d = forecast.daylight) {
            is DaylightState.BeforeSunrise -> {
                Column {
                    Text("Before Sunrise", style = MaterialTheme.typography.labelMedium)
                    Text("${d.timeUntilSunriseFormatted} until sunrise (${d.sunriseFormatted})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Day length today: ${d.dayLengthFormatted} (Sunset: ${d.sunsetFormatted})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is DaylightState.Daylight -> {
                Column {
                    Text("Daylight", style = MaterialTheme.typography.labelMedium)
                    Text("${d.timeUntilSunsetFormatted} until sunset (${d.sunsetFormatted})", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Day length today: ${d.dayLengthFormatted} (Sunrise was ${d.sunriseFormatted})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is DaylightState.AfterSunset -> {
                Column {
                    Text("After Sunset", style = MaterialTheme.typography.labelMedium)
                    Text("Sunset was at ${d.sunsetFormatted}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Day length was ${d.dayLengthFormatted} (Sunrise was ${d.sunriseFormatted})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DaylightState.Unavailable -> {
                Text("Daylight tracking is currently unavailable", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun BestRunningPersonaCard(forecast: ForecastUi) {
    CardShell("Outdoor Running Window") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                when (val run = forecast.bestRunning) {
                    is BestRunningResult.Optimal -> {
                        Text(
                            text = "Best window: ${run.startHourLabel} – ${run.endHourLabel}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Score: ${run.score}/100 • ${run.reasons.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is BestRunningResult.NoGoodWindow -> {
                        Text(
                            text = "No ideal running window",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reason: ${run.reason}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
