package com.weatherengine.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
val GlassSurfaceDark = Color(0x28FFFFFF) // 16% transluscent white glass
val GlassBorderDark = Color(0x33A5B4FC)  // soft luminous indigo rim
val GlassSurfaceLight = Color(0xF2FFFFFF)
val GlassBorderLight = Color(0x1F000000)

val AuroraCyan = Color(0xFF38BDF8)
val AuroraIndigo = Color(0xFF818CF8)
val AuroraViolet = Color(0xFFA855F7)
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
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bgBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B111E),
                Color(0xFF0F172A),
                Color(0xFF06090F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F5FF),
                Color(0xFFE2E8F0),
                Color(0xFFFFFFFF)
            )
        )
    }

    Box(
        modifier = modifier
            .background(bgBrush),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassCardShape,
    elevation: Dp = 0.dp,
    borderBrush: Brush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.25f),
            Color.White.copy(alpha = 0.05f)
        )
    ),
    containerColor: Color = if (androidx.compose.foundation.isSystemInDarkTheme()) {
        Color(0xFF161F30).copy(alpha = 0.70f)
    } else {
        Color.White.copy(alpha = 0.88f)
    },
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(containerColor)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = borderBrush
                ),
                shape = shape
            ),
        content = content
    )
}
