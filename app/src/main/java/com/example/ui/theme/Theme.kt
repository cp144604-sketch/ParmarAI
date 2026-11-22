package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ParmarColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepAzure,
    onPrimaryContainer = CrispIce,
    secondary = CobaltGlow,
    onSecondary = CrispIce,
    secondaryContainer = ElevatedCard,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonCyan,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = CrispIce,
    surface = DeepSpace,
    onSurface = CrispIce,
    surfaceVariant = MidnightSurface,
    onSurfaceVariant = CoolSlate,
    outline = SubtleBorder,
    error = CrimsonAlert,
    onError = ObsidianBlack
)

private val ParmarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ParmarAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParmarColorScheme,
        typography = ParmarTypography,
        shapes = ParmarShapes,
        content = content
    )
}
