package com.weatherengine.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherengine.app.core.forecast.DayForecastUi
import com.weatherengine.app.ui.theme.GlassCard

@Composable
fun SevenDayForecastCard(days: List<DayForecastUi>, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "7-Day Forecast",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            days.forEachIndexed { index, day ->
                DayForecastRow(day = day)
                if (index < days.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
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
    val animatedStart by animateFloatAsState(
        targetValue = fractionStart.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "rangeStart"
    )
    val animatedEnd by animateFloatAsState(
        targetValue = fractionEnd.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "rangeEnd"
    )

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        val fillWidthFraction = (animatedEnd - animatedStart).coerceAtLeast(0.05f)

        Row(modifier = Modifier.fillMaxWidth()) {
            if (animatedStart > 0f) {
                Spacer(modifier = Modifier.weight(animatedStart))
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
            val remaining = (1f - animatedEnd).coerceAtLeast(0f)
            if (remaining > 0f) {
                Spacer(modifier = Modifier.weight(remaining))
            }
        }
    }
}
