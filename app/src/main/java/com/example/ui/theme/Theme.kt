package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = SaffronAccent,
    onTertiary = Color.White,
    background = ParchmentBackground,
    onBackground = Color(0xFF121C18),
    surface = SurfaceCard,
    onSurface = Color(0xFF121C18),
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Color(0xFF43544D),
    error = StatusError,
    onError = Color.White,
    outline = Color(0xFFC2CEC8)
)

private val DarkColors = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = Color(0xFF003828),
    primaryContainer = EmeraldPrimary,
    onPrimaryContainer = EmeraldContainer,
    secondary = EmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A2800),
    onSecondaryContainer = SaffronContainer,
    tertiary = SaffronAccent,
    background = DarkBackground,
    onBackground = Color(0xFFE6F0EC),
    surface = DarkSurface,
    onSurface = Color(0xFFE6F0EC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4C6BE),
    error = Color(0xFFFFB4AB),
    outline = Color(0xFF3E524A)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun PrayagDeenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
