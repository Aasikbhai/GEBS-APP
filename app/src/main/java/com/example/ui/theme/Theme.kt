package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class GlassPalette(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundMid: Color,
    val backgroundBottom: Color,
    val glassCard: Color,
    val glassPopup: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
)

val LocalGlassPalette = staticCompositionLocalOf {
    GlassPalette(
        isDark = false,
        backgroundTop = LightAuroraTop,
        backgroundMid = LightAuroraMid,
        backgroundBottom = LightAuroraBottom,
        glassCard = LightGlassSurface,
        glassPopup = LightGlassElevated,
        glassBorderTop = LightGlassBorderTop,
        glassBorderBottom = LightGlassBorderBottom,
        orbPrimary = Color(0x5500B4D8),
        orbSecondary = Color(0x440F4C81),
        orbAccent = Color(0x4410B981),
        success = EmeraldMint,
        warning = AmberGold,
        danger = CoralRose
    )
}

private val LightColors = lightColorScheme(
    primary = DeepSapphire,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEFF),
    onPrimaryContainer = MidnightSapphire,
    secondary = ElectricCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF5FC),
    onSecondaryContainer = Color(0xFF003642),
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = LightAuroraMid,
    onBackground = LightTextPrimary,
    surface = LightGlassSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFE2EFF9),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFF94A3B8),
    error = CoralRose,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = MidnightSapphire,
    primaryContainer = Color(0xFF133A63),
    onPrimaryContainer = Color(0xFFD6EEFF),
    secondary = SkyMist,
    onSecondary = MidnightSapphire,
    secondaryContainer = Color(0xFF0F4C6E),
    onSecondaryContainer = Color(0xFFCFF5FC),
    tertiary = Color(0xFF818CF8),
    onTertiary = MidnightSapphire,
    background = DarkAuroraMid,
    onBackground = DarkTextPrimary,
    surface = DarkGlassSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF1A3358),
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF475569),
    error = Color(0xFFF87171),
    onError = MidnightSapphire
)

private val CampusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun CampusPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val glassPalette = if (darkTheme) {
        GlassPalette(
            isDark = true,
            backgroundTop = DarkAuroraTop,
            backgroundMid = DarkAuroraMid,
            backgroundBottom = DarkAuroraBottom,
            glassCard = DarkGlassSurface,
            glassPopup = DarkGlassElevated,
            glassBorderTop = DarkGlassBorderTop,
            glassBorderBottom = DarkGlassBorderBottom,
            orbPrimary = Color(0x4400B4D8),
            orbSecondary = Color(0x554F46E5),
            orbAccent = Color(0x3310B981),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            danger = Color(0xFFF87171)
        )
    } else {
        GlassPalette(
            isDark = false,
            backgroundTop = LightAuroraTop,
            backgroundMid = LightAuroraMid,
            backgroundBottom = LightAuroraBottom,
            glassCard = LightGlassSurface,
            glassPopup = LightGlassElevated,
            glassBorderTop = LightGlassBorderTop,
            glassBorderBottom = LightGlassBorderBottom,
            orbPrimary = Color(0x5500B4D8),
            orbSecondary = Color(0x384F46E5),
            orbAccent = Color(0x3810B981),
            success = EmeraldMint,
            warning = AmberGold,
            danger = CoralRose
        )
    }

    CompositionLocalProvider(LocalGlassPalette provides glassPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CampusTypography,
            shapes = CampusShapes,
            content = content
        )
    }
}
