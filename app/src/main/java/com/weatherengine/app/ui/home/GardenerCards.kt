package com.weatherengine.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Water
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
import com.weatherengine.app.ui.theme.EmeraldCalm
import com.weatherengine.app.ui.theme.ErrorRed
import com.weatherengine.app.ui.theme.GlassCard
import com.weatherengine.app.ui.theme.SuccessGreen

// ── Soil Moisture ─────────────────────────────────────────────────────────────
@Composable
fun SoilMoistureCard(soil: WidgetUi.SoilMoisture, modifier: Modifier = Modifier) {
    val levelColor = when (soil.level) {
        "Wet" -> EmeraldCalm
        "Moist" -> SuccessGreen
        "Dry" -> AmberWarning
        else -> MaterialTheme.colorScheme.primary
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Soil moisture: ${soil.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Grass,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Soil Moisture",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = soil.level,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${soil.precip7dMm} mm rain (7d) · ${soil.humidityPercent}% humidity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = soil.advice,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Rainfall Prediction ───────────────────────────────────────────────────────
@Composable
fun RainfallPredictionCard(rainfall: WidgetUi.RainfallPrediction, modifier: Modifier = Modifier) {
    val outlookColor = when {
        rainfall.rainyDayCount >= 5 -> EmeraldCalm
        rainfall.rainyDayCount >= 2 -> MaterialTheme.colorScheme.primary
        else -> AmberWarning
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Rainfall forecast: ${rainfall.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Water,
                    contentDescription = null,
                    tint = outlookColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Rainfall Forecast",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text(
                        text = "${rainfall.totalMm} mm",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total (${rainfall.forecastDays}d)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "${rainfall.rainyDayCount} days",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = outlookColor
                    )
                    Text(
                        text = "Rainy days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rainfall.outlook,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ── Frost Alert ───────────────────────────────────────────────────────────────
@Composable
fun FrostAlertCard(frost: WidgetUi.FrostAlert, modifier: Modifier = Modifier) {
    val alertColor = when (frost.alertLevel) {
        "Hard Frost" -> ErrorRed
        "Frost Risk" -> AmberWarning
        else -> SuccessGreen
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Frost alert: ${frost.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Frost Alert",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = frost.alertLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = alertColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = frost.advice,
                style = MaterialTheme.typography.bodyMedium
            )
            if (frost.alertActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Frost risk: ${frost.frostRiskDayCount}d · Hard frost: ${frost.hardFrostDayCount}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Planting Guidance ─────────────────────────────────────────────────────────
@Composable
fun PlantingGuidanceCard(planting: WidgetUi.PlantingGuidance, modifier: Modifier = Modifier) {
    val guidanceColor = when {
        planting.hasFrostRisk -> ErrorRed
        planting.guidance.contains("Good", ignoreCase = true) -> SuccessGreen
        else -> MaterialTheme.colorScheme.primary
    }
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Planting guidance: ${planting.message}" },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiNature,
                    contentDescription = null,
                    tint = guidanceColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Planting Guidance",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = planting.guidance,
                style = MaterialTheme.typography.bodyMedium,
                color = guidanceColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Avg ${planting.avgMinTempC}°C – ${planting.avgMaxTempC}°C · ${planting.totalRainMm7d} mm rain (7d)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
