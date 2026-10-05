package com.weatherengine.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.persona.HourlyRowUi
import com.weatherengine.app.ui.home.weatherIconFor

@Composable
fun HourlyForecastStrip(
    hours: List<HourlyRowUi>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = "Hourly Forecast (24 Hours)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (hours.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hourly forecast is not available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hours) { hour ->
                        HourlyItem(hour = hour)
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyItem(hour: HourlyRowUi) {
    val isNow = hour.timeLabel == "Now"
    val containerBg = if (isNow) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    val a11yDesc = "${hour.timeLabel}: ${hour.tempFormatted}, ${hour.condition}" +
            if (hour.precipProb != null && hour.precipProb > 0) ", ${hour.precipProb}% rain chance" else ""

    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics { contentDescription = a11yDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = hour.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
            color = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Icon(
            imageVector = weatherIconFor(hour.iconKey),
            contentDescription = hour.condition,
            modifier = Modifier.size(24.dp),
            tint = if (isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        if (hour.precipProb != null && hour.precipProb > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${hour.precipProb}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
            text = hour.tempFormatted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

