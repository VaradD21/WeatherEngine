package com.weatherengine.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherengine.app.core.forecast.ForecastUi
import com.weatherengine.app.persona.HeatStatus
import com.weatherengine.app.persona.PersonaRules
import com.weatherengine.app.persona.PersonaWidgetType
import com.weatherengine.app.ui.theme.AmberWarning
import com.weatherengine.app.ui.theme.ErrorRed
import com.weatherengine.app.ui.theme.SuccessGreen

@Composable
fun RenderPersonaWidget(
    widgetType: PersonaWidgetType,
    forecast: ForecastUi,
    modifier: Modifier = Modifier
) {
    when (widgetType) {
        PersonaWidgetType.AQI -> AqiPersonaCard(forecast)
        PersonaWidgetType.UV -> UvPersonaCard(forecast)
        PersonaWidgetType.HUMIDITY -> HumidityPersonaCard(forecast)
        PersonaWidgetType.POLLEN -> PollenPersonaCard(forecast)
        PersonaWidgetType.SUN -> SunPersonaCard(forecast)
        PersonaWidgetType.BEST_RUNNING -> BestRunningPersonaCard(forecast)
        PersonaWidgetType.WIND -> WindPersonaCard(forecast)
        PersonaWidgetType.HEAT -> HeatPersonaCard(forecast)
        PersonaWidgetType.VISIBILITY -> VisibilityPersonaCard(forecast)
        PersonaWidgetType.STORM_FOG -> StormFogPersonaCard(forecast)
        PersonaWidgetType.COMMUTE_RAIN -> CommuteRainPersonaCard(forecast)
        PersonaWidgetType.TRAFFIC -> TrafficPersonaCard()
    }
}

@Composable
fun WindPersonaCard(forecast: ForecastUi) {
    val speed = forecast.current.windSpeedKmh
    val gusts = forecast.current.windGustsKmh
    val category = PersonaRules.windCategory(speed)

    CardShell("Wind Conditions") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                if (speed != null) {
                    Text(
                        text = "$speed km/h — $category",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (gusts != null && gusts > speed) {
                        Text(
                            text = "Peak Gusts: $gusts km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text("Wind data is currently unavailable", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun HeatPersonaCard(forecast: ForecastUi) {
    CardShell("Heat Advisory") {
        when (val heat = forecast.heat) {
            is HeatStatus.Alert -> {
                AlertRow(
                    isActive = true,
                    title = heat.message,
                    subtitle = heat.note,
                    activeColor = ErrorRed
                )
            }
            is HeatStatus.Caution -> {
                AlertRow(
                    isActive = true,
                    title = heat.message,
                    subtitle = heat.note,
                    activeColor = AmberWarning
                )
            }
            HeatStatus.None -> {
                AlertRow(
                    isActive = false,
                    title = "No Heat Risk Today",
                    subtitle = "Temperatures are within comfortable limits.",
                    activeColor = SuccessGreen
                )
            }
        }
    }
}

@Composable
fun VisibilityPersonaCard(forecast: ForecastUi) {
    val metres = forecast.current.visibilityMeters
    val category = PersonaRules.visibilityCategory(metres)

    CardShell("Road Visibility") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.padding(start = 10.dp)) {
                if (metres != null) {
                    val km = String.format("%.1f", metres / 1000.0)
                    Text(
                        text = "$km km — $category",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("Visibility data is currently unavailable", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun StormFogPersonaCard(forecast: ForecastUi) {
    val status = forecast.stormFog
    CardShell("Storm & Fog Alerts") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (status.alertActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = if (status.alertActive) "Active Weather Alert" else "Clear Conditions",
                tint = if (status.alertActive) ErrorRed else SuccessGreen,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = if (status.alertActive) "ALERT ACTIVE" else "Conditions Clear",
                    fontWeight = FontWeight.Bold,
                    color = if (status.alertActive) ErrorRed else SuccessGreen,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CommuteRainPersonaCard(forecast: ForecastUi) {
    val commute = forecast.commute
    CardShell("Commute Rain Outlook (Next 6h)") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "${commute.advice} (${commute.peakRainChance}%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Peak rain chance expected around ${commute.peakHourLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TrafficPersonaCard() {
    CardShell("Live Traffic") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Traffic,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "Live traffic is not available yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Integration pending in a future release.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun AlertRow(
    isActive: Boolean,
    title: String,
    subtitle: String,
    activeColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isActive) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = if (isActive) "Active Alert" else "Clear",
            tint = if (isActive) activeColor else SuccessGreen,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isActive) activeColor else SuccessGreen
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
