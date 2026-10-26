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

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KathaTheme(
    themeMode: String = "dark",
    accentTheme: String = "velvet_rose",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val (lightPrimary, darkPrimary, secondaryAccent) = when (accentTheme) {
        "midnight_violet" -> Triple(MidnightVioletLightPrimary, MidnightVioletDarkPrimary, MidnightVioletSecondary)
        "golden_amber" -> Triple(GoldenAmberLightPrimary, GoldenAmberDarkPrimary, GoldenAmberSecondary)
        "emerald_jade" -> Triple(EmeraldJadeLightPrimary, EmeraldJadeDarkPrimary, EmeraldJadeSecondary)
        "ocean_cyan" -> Triple(OceanCyanLightPrimary, OceanCyanDarkPrimary, OceanCyanSecondary)
        else -> Triple(VelvetRoseLightPrimary, VelvetRoseDarkPrimary, VelvetRoseSecondary)
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = darkPrimary,
            onPrimary = Color(0xFF1A0914),
            primaryContainer = darkPrimary.copy(alpha = 0.22f),
            onPrimaryContainer = DarkOnSurface,
            secondary = secondaryAccent,
            onSecondary = Color(0xFF1E1305),
            secondaryContainer = secondaryAccent.copy(alpha = 0.2f),
            onSecondaryContainer = DarkOnSurface,
            tertiary = Color(0xFF38BDF8),
            background = DarkBackground,
            onBackground = DarkOnSurface,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceContainer,
            onSurfaceVariant = DarkOnSurfaceVariant,
            surfaceContainer = DarkSurfaceContainer,
            surfaceContainerHigh = DarkSurfaceHigh,
            outline = Color(0xFF4B3D63)
        )
    } else {
        lightColorScheme(
            primary = lightPrimary,
            onPrimary = Color.White,
            primaryContainer = lightPrimary.copy(alpha = 0.14f),
            onPrimaryContainer = LightOnSurface,
            secondary = secondaryAccent,
            onSecondary = Color.White,
            secondaryContainer = secondaryAccent.copy(alpha = 0.16f),
            onSecondaryContainer = LightOnSurface,
            tertiary = Color(0xFF0284C7),
            background = LightBackground,
            onBackground = LightOnSurface,
            surface = LightSurface,
            onSurface = LightOnSurface,
            surfaceVariant = LightSurfaceContainer,
            onSurfaceVariant = LightOnSurfaceVariant,
            surfaceContainer = LightSurfaceContainer,
            surfaceContainerHigh = LightSurfaceHigh,
            outline = Color(0xFFC8BBD6)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
