package com.weatherengine.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.core.Formatters
import com.weatherengine.app.core.models.WidgetUi
import com.weatherengine.app.ui.theme.AmberWarning
import com.weatherengine.app.ui.theme.AqiFair
import com.weatherengine.app.ui.theme.AqiGood
import com.weatherengine.app.ui.theme.AqiModerate
import com.weatherengine.app.ui.theme.AqiPoor
import com.weatherengine.app.ui.theme.AqiVeryPoor
import com.weatherengine.app.ui.theme.ErrorRed
import com.weatherengine.app.ui.theme.SuccessGreen

@Composable
fun RenderWidgetCard(widget: WidgetUi, modifier: Modifier = Modifier) {
    when (widget) {
        is WidgetUi.Aqi -> AqiCard(widget, modifier)
        is WidgetUi.Humidity -> HumidityCard(widget, modifier)
        is WidgetUi.Uv -> UvCard(widget, modifier)
        is WidgetUi.SunriseSunset -> SunriseSunsetCard(widget, modifier)
        is WidgetUi.Wind -> WindCard(widget, modifier)
        is WidgetUi.HeatAlert -> HeatAlertCard(widget, modifier)
        is WidgetUi.Traffic -> TrafficCard(widget, modifier)
        is WidgetUi.Visibility -> VisibilityCard(widget, modifier)
        is WidgetUi.StormFog -> StormFogCard(widget, modifier)
        is WidgetUi.StatusOnly -> StatusOnlyCard(widget, modifier)
        is WidgetUi.Unsupported -> UnsupportedCard(widget, modifier)
    }
}

@Composable
private fun CardShell(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun AqiCard(data: WidgetUi.Aqi, modifier: Modifier = Modifier) {
    val aqiInfo = Formatters.aqiInfo(data.aqi)
    val color = when (aqiInfo.level) {
        1 -> AqiGood
        2 -> AqiFair
        3 -> AqiModerate
        4 -> AqiPoor
        5 -> AqiVeryPoor
        else -> MaterialTheme.colorScheme.outline
    }
    CardShell("Air Quality (AQI)", modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${data.aqi} — ${aqiInfo.label}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "Category: ${data.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun HumidityCard(data: WidgetUi.Humidity, modifier: Modifier = Modifier) {
    CardShell("Humidity", modifier) {
        Text(
            text = "${data.humidityPercent}%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun UvCard(data: WidgetUi.Uv, modifier: Modifier = Modifier) {
    CardShell("UV Index", modifier) {
        val uv = data.uvIndex
        if (uv != null) {
            Text(
                text = String.format("%.1f", uv),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        data.message?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SunriseSunsetCard(data: WidgetUi.SunriseSunset, modifier: Modifier = Modifier) {
    CardShell("Sunrise & Sunset", modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Sunrise", style = MaterialTheme.typography.labelMedium)
                Text(Formatters.formatClock(data.sunrise), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Sunset", style = MaterialTheme.typography.labelMedium)
                Text(Formatters.formatClock(data.sunset), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WindCard(data: WidgetUi.Wind, modifier: Modifier = Modifier) {
    val kmh = Formatters.windMsToKmh(data.speedMetersPerSecond)
    CardShell("Wind Speed", modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "${data.speedMetersPerSecond} m/s",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${kmh ?: "—"} km/h)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeatAlertCard(data: WidgetUi.HeatAlert, modifier: Modifier = Modifier) {
    CardShell("Heat Alert", modifier) {
        AlertContent(
            isActive = data.alertActive,
            activeTitle = "HEAT ALERT ACTIVE",
            inactiveTitle = "No Active Heat Alert",
            message = "${data.temperatureCelsius}°C — ${data.message}"
        )
    }
}

@Composable
private fun StormFogCard(data: WidgetUi.StormFog, modifier: Modifier = Modifier) {
    CardShell("Storm & Fog Alert", modifier) {
        AlertContent(
            isActive = data.alertActive,
            activeTitle = "STORM/FOG ALERT ACTIVE",
            inactiveTitle = "No Severe Conditions",
            message = data.message
        )
    }
}

@Composable
private fun AlertContent(
    isActive: Boolean,
    activeTitle: String,
    inactiveTitle: String,
    message: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = if (isActive) "Active Alert" else "Clear",
            tint = if (isActive) ErrorRed else SuccessGreen,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = if (isActive) activeTitle else inactiveTitle,
                fontWeight = FontWeight.Bold,
                color = if (isActive) ErrorRed else SuccessGreen,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TrafficCard(data: WidgetUi.Traffic, modifier: Modifier = Modifier) {
    CardShell("Traffic Conditions", modifier) {
        if (data.status == "mocked") {
            StatusBadge("Demo data — integration pending", AmberWarning, Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(data.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun VisibilityCard(data: WidgetUi.Visibility, modifier: Modifier = Modifier) {
    CardShell("Visibility", modifier) {
        val km = data.visibilityMeters / 1000.0
        Text(
            text = "$km km (${data.category})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusOnlyCard(data: WidgetUi.StatusOnly, modifier: Modifier = Modifier) {
    CardShell(Formatters.widgetTitle(data.type), modifier) {
        when (data.status) {
            "mocked" -> {
                StatusBadge("Demo data — integration pending", AmberWarning, Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(data.message, style = MaterialTheme.typography.bodyMedium)
            }
            "unavailable" -> {
                StatusBadge("Unavailable", AmberWarning, Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(data.message, style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(
                    text = "Temporarily unavailable",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (data.message.isNotBlank() && data.message != "Temporarily unavailable") {
                    Text(text = data.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun UnsupportedCard(data: WidgetUi.Unsupported, modifier: Modifier = Modifier) {
    CardShell(Formatters.widgetTitle(data.type), modifier) {
        Text(
            text = "Not supported in this app version",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun StatusBadge(text: String, background: Color, textCol: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = textCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
