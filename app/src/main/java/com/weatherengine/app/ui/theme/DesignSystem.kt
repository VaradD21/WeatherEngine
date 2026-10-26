package com.weatherengine.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Modern Aurora & Frosted Glass Palette
val ObsidianBg = Color(0xFF090D16)
val DeepNavy = Color(0xFF0E1626)
val GlassSurfaceDark = Color(0x28FFFFFF) // 16% translucent white glass
val GlassBorderDark = Color(0x33A5B4FC)  // soft luminous indigo rim
val GlassSurfaceLight = Color(0xF2FFFFFF)
val GlassBorderLight = Color(0x1F000000)

val AuroraCyan = Color(0xFF38BDF8)
val AuroraIndigo = Color(0xFF818CF8)
val AuroraViolet = Color(0xFFA855F7)
val OceanBlue = Color(0xFF0284C7)
val EmeraldCalm = Color(0xFF10B981)
val CoralAmber = Color(0xFFF59E0B)
val RoseAlert = Color(0xFFF43F5E)

val GlassPillShape = RoundedCornerShape(50.dp)
val GlassCardShape = RoundedCornerShape(26.dp)

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val infiniteTransition = rememberInfiniteTransition(label = "auroraShift")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraShift"
    )

    val bgBrush = if (isDark) {
        val midColor1 = Color(0xFF0D1629)
        val midColor2 = Color(0xFF132240)
        val shiftColor = Color(
            red = midColor1.red + (midColor2.red - midColor1.red) * animOffset,
            green = midColor1.green + (midColor2.green - midColor1.green) * animOffset,
            blue = midColor1.blue + (midColor2.blue - midColor1.blue) * animOffset,
            alpha = 1f
        )
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070B14),
                shiftColor,
                Color(0xFF0F1A30),
                Color(0xFF080D18)
            )
        )
    } else {
        val light1 = Color(0xFFEFF6FF)
        val light2 = Color(0xFFE0E7FF)
        val shiftColor = Color(
            red = light1.red + (light2.red - light1.red) * animOffset,
            green = light1.green + (light2.green - light1.green) * animOffset,
            blue = light1.blue + (light2.blue - light1.blue) * animOffset,
            alpha = 1f
        )
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F7FF),
                shiftColor,
                Color(0xFFF8FAFC)
            )
        )
    }

    Box(
        modifier = modifier.background(bgBrush),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    elevation: Dp = 0.dp,
    borderBrush: Brush? = null,
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val resolvedBorder = borderBrush ?: Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.30f else 0.65f),
            Color.White.copy(alpha = if (isDark) 0.10f else 0.25f),
            Color.White.copy(alpha = 0.02f)
        )
    )
    val resolvedContainer = containerColor ?: if (isDark) {
        Color(0xFF131D31).copy(alpha = 0.68f)
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(resolvedContainer)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = resolvedBorder
                ),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun GlassBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(GlassPillShape)
            .background(backgroundColor)
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shape = GlassPillShape
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        content = content
    )
}
