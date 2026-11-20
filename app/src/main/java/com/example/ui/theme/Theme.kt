package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TorusToneDarkColorScheme = darkColorScheme(
    primary = BrassGold,
    onPrimary = OnBrassGold,
    primaryContainer = BrassGoldDim,
    onPrimaryContainer = BrassGoldBright,
    secondary = HarmonicCyan,
    onSecondary = OnHarmonicCyan,
    secondaryContainer = HarmonicCyanDim,
    onSecondaryContainer = HarmonicCyan,
    tertiary = MinorRose,
    onTertiary = OnMinorRose,
    tertiaryContainer = MinorRoseDim,
    onTertiaryContainer = MinorRose,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianDeep,
    onSurface = TextPrimary,
    surfaceVariant = AnodizedSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianDeep,
    surfaceContainer = AnodizedSurface,
    surfaceContainerHigh = AnodizedElevated,
    surfaceContainerHighest = AnodizedHighlight,
    outline = OutlineBright,
    outlineVariant = OutlineSubtle
)

private val TorusToneShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TorusToneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TorusToneDarkColorScheme,
        typography = TorusToneTypography,
        shapes = TorusToneShapes,
        content = content
    )
}
