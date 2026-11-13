package com.aibuilt.castlenoads.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Color0B = Color(0xFF0B0B0F)

private val CastleDarkScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color0B,
    primaryContainer = GoldDark,
    onPrimaryContainer = Parchment,
    secondary = Parchment,
    onSecondary = Color0B,
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnBackground,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = NightOnBackground,
    outline = NightOutline,
    error = ErrorRed,
    onError = Parchment
)

private val CastleLightScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = Parchment,
    primaryContainer = Gold,
    onPrimaryContainer = Color0B,
    secondary = Color0B,
    onSecondary = Parchment,
    background = DayBackground,
    onBackground = DayOnBackground,
    surface = DaySurface,
    onSurface = DayOnBackground,
    surfaceVariant = DaySurfaceHigh,
    onSurfaceVariant = DayOnBackground,
    outline = DayOutline,
    error = ErrorRed,
    onError = Parchment
)

private val CastleTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.4.sp)
)

@Composable
fun CastleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CastleDarkScheme else CastleLightScheme,
        typography = CastleTypography,
        content = content
    )
}
