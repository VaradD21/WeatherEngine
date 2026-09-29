package com.weatherengine.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.core.forecast.CurrentWeatherUi
import com.weatherengine.app.core.forecast.DayForecastUi
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.ui.theme.AqiFair
import com.weatherengine.app.ui.theme.AqiGood
import com.weatherengine.app.ui.theme.AqiModerate
import com.weatherengine.app.ui.theme.AqiPoor
import com.weatherengine.app.ui.theme.AqiVeryPoor

@Composable
fun CurrentConditionsCard(current: CurrentWeatherUi, updatedAt: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = current.temperatureFormatted,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 56.sp
                    )
                    Text(
                        text = current.condition,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (current.apparentTemperatureFormatted.isNotBlank()) {
                        Text(
                            text = current.apparentTemperatureFormatted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Icon(
                    imageVector = weatherIconFor(current.iconKey),
                    contentDescription = current.condition,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(Icons.Default.WaterDrop, "Humidity", current.humidityFormatted)
                MetricChip(Icons.Default.Air, "Wind", current.windFormatted)
                MetricChip(Icons.Default.Cloud, "Precip", current.precipitationFormatted)
                if (current.aqi != null) {
                    MetricChip(
                        Icons.Default.Thermostat,
                        "AQI",
                        "${current.aqi} (${current.aqiCategory ?: ""})"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = updatedAt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun SevenDayForecastCard(days: List<DayForecastUi>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            days.forEachIndexed { index, day ->
                DayForecastRow(day = day)
                if (index < days.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DayForecastRow(day: DayForecastUi, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(76.dp)
        )

        Row(
            modifier = Modifier.width(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = weatherIconFor(day.iconKey),
                contentDescription = day.condition,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            if (day.precipitationProbability != null && day.precipitationProbability > 0) {
                Text(
                    text = "${day.precipitationProbability}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = day.minTempFormatted,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )

        TemperatureRangeBar(
            fractionStart = day.rangeFractionStart,
            fractionEnd = day.rangeFractionEnd,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        Text(
            text = day.maxTempFormatted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
fun TemperatureRangeBar(
    fractionStart: Float,
    fractionEnd: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        val safeStart = fractionStart.coerceIn(0f, 1f)
        val safeEnd = fractionEnd.coerceIn(safeStart, 1f)
        val fillWidthFraction = (safeEnd - safeStart).coerceAtLeast(0.05f)

        Row(modifier = Modifier.fillMaxWidth()) {
            if (safeStart > 0f) {
                Spacer(modifier = Modifier.weight(safeStart))
            }
            Box(
                modifier = Modifier
                    .weight(fillWidthFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4FC3F7),
                                Color(0xFFFFB74D),
                                Color(0xFFFF7043)
                            )
                        )
                    )
            )
            val remaining = (1f - safeEnd).coerceAtLeast(0f)
            if (remaining > 0f) {
                Spacer(modifier = Modifier.weight(remaining))
            }
        }
    }
}

@Composable
fun OpenMeteoAttributionCard(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri("https://open-meteo.com/") },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Weather data by Open-Meteo.com (Tap to visit)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PersonalizeBannerCard(
    onNavigateAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Personalize Your Weather",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Log in or sign up to activate custom personas like Outdoor Fitness, Commuter, and Health Alerts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onNavigateAuth,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In / Sign Up")
            }
        }
    }
}

@Composable
private fun MetricChip(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun weatherIconFor(key: String): ImageVector {
    return when (key) {
        "clear_day", "mainly_clear_day" -> Icons.Default.WbSunny
        "clear_night", "mainly_clear_night" -> Icons.Default.WbSunny
        "partly_cloudy_day", "partly_cloudy_night", "cloudy" -> Icons.Default.Cloud
        "rain", "heavy_rain", "drizzle", "rain_showers" -> Icons.Default.WaterDrop
        "thunderstorm" -> Icons.Default.Air
        else -> Icons.Default.Cloud
    }
}
