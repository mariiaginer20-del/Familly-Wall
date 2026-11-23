package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FamilyDarkPrimary,
    onPrimary = FamilyDarkOnPrimary,
    primaryContainer = FamilyDarkPrimaryContainer,
    onPrimaryContainer = FamilyDarkOnPrimaryContainer,
    secondary = FamilyDarkSecondary,
    onSecondary = FamilyDarkOnSecondary,
    background = FamilyDarkBackground,
    surface = FamilyDarkSurface,
    onSurface = FamilyDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = FamilyPrimary,
    onPrimary = FamilyOnPrimary,
    primaryContainer = FamilyPrimaryContainer,
    onPrimaryContainer = FamilyOnPrimaryContainer,
    secondary = FamilySecondary,
    onSecondary = FamilyOnSecondary,
    secondaryContainer = FamilySecondaryContainer,
    onSecondaryContainer = FamilyOnSecondaryContainer,
    tertiary = FamilyTertiary,
    onTertiary = FamilyOnTertiary,
    tertiaryContainer = FamilyTertiaryContainer,
    onTertiaryContainer = FamilyOnTertiaryContainer,
    background = FamilyBackground,
    onBackground = FamilyOnBackground,
    surface = FamilySurface,
    onSurface = FamilyOnSurface,
    surfaceVariant = FamilySurfaceVariant,
    onSurfaceVariant = FamilyOnSurfaceVariant
)

@Composable
fun FamilyWallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cohesive custom family branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
