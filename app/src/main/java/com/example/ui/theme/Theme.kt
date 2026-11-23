package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = FamilyDarkPrimary,
    onPrimary = FamilyDarkOnPrimary,
    primaryContainer = FamilyDarkPrimaryContainer,
    onPrimaryContainer = FamilyDarkOnPrimaryContainer,
    secondary = FamilyDarkSecondary,
    onSecondary = FamilyDarkOnSecondary,
    secondaryContainer = FamilyDarkSecondaryContainer,
    onSecondaryContainer = FamilyDarkOnSecondaryContainer,
    background = FamilyDarkBackground,
    onBackground = FamilyDarkOnBackground,
    surface = FamilyDarkSurface,
    onSurface = FamilyDarkOnSurface,
    surfaceVariant = FamilyDarkSurfaceVariant,
    onSurfaceVariant = FamilyDarkOnSurfaceVariant,
    outline = FamilyDarkOutline,
    outlineVariant = FamilyDarkOutlineVariant
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
    onSurfaceVariant = FamilyOnSurfaceVariant,
    surfaceContainerLow = FamilySurfaceContainerLow,
    surfaceContainer = FamilySurfaceContainer,
    outline = FamilyOutline,
    outlineVariant = FamilyOutlineVariant,
    error = FamilyError,
    onError = FamilyOnError,
    errorContainer = FamilyErrorContainer,
    onErrorContainer = FamilyOnErrorContainer
)

val FamilyShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FamilyWallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FamilyShapes,
        content = content
    )
}
