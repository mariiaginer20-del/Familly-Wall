package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.preferences.AppDesignStyle
import com.example.data.preferences.ThemeDarkMode

val LocalDesignStyle = compositionLocalOf { AppDesignStyle.EDITORIAL }

private val EditorialDarkColorScheme = darkColorScheme(
    primary = FamilyDarkPrimary,
    onPrimary = FamilyDarkOnPrimary,
    primaryContainer = FamilyDarkPrimaryContainer,
    onPrimaryContainer = FamilyDarkOnPrimaryContainer,
    secondary = FamilyDarkSecondary,
    onSecondary = FamilyDarkOnSecondary,
    secondaryContainer = FamilyDarkSecondaryContainer,
    onSecondaryContainer = FamilyDarkOnSecondaryContainer,
    tertiary = FamilyDarkTertiary,
    onTertiary = FamilyDarkOnTertiary,
    tertiaryContainer = FamilyDarkTertiaryContainer,
    onTertiaryContainer = FamilyDarkOnTertiaryContainer,
    background = FamilyDarkBackground,
    onBackground = FamilyDarkOnBackground,
    surface = FamilyDarkSurface,
    onSurface = FamilyDarkOnSurface,
    surfaceVariant = FamilyDarkSurfaceVariant,
    onSurfaceVariant = FamilyDarkOnSurfaceVariant,
    surfaceContainerLow = FamilyDarkSurfaceContainerLow,
    surfaceContainer = FamilyDarkSurfaceContainer,
    outline = FamilyDarkOutline,
    outlineVariant = FamilyDarkOutlineVariant,
    error = FamilyDarkError,
    onError = FamilyDarkOnError,
    errorContainer = FamilyDarkErrorContainer,
    onErrorContainer = FamilyDarkOnErrorContainer
)

private val EditorialLightColorScheme = lightColorScheme(
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

private val BentoDarkColorScheme = darkColorScheme(
    primary = BentoDarkPrimary,
    onPrimary = BentoDarkOnPrimary,
    primaryContainer = BentoDarkPrimaryContainer,
    onPrimaryContainer = BentoDarkOnPrimaryContainer,
    secondary = BentoDarkSecondary,
    onSecondary = BentoDarkOnSecondary,
    secondaryContainer = BentoDarkSecondaryContainer,
    onSecondaryContainer = BentoDarkOnSecondaryContainer,
    tertiary = BentoDarkTertiary,
    onTertiary = BentoDarkOnTertiary,
    tertiaryContainer = BentoDarkTertiaryContainer,
    onTertiaryContainer = BentoDarkOnTertiaryContainer,
    background = BentoDarkBackground,
    onBackground = BentoDarkOnBackground,
    surface = BentoDarkSurface,
    onSurface = BentoDarkOnSurface,
    surfaceVariant = BentoDarkSurfaceVariant,
    onSurfaceVariant = BentoDarkOnSurfaceVariant,
    surfaceContainerLow = BentoDarkSurfaceContainerLow,
    surfaceContainer = BentoDarkSurfaceContainer,
    outline = BentoDarkOutline,
    outlineVariant = BentoDarkOutlineVariant,
    error = FamilyDarkError,
    onError = FamilyDarkOnError,
    errorContainer = FamilyDarkErrorContainer,
    onErrorContainer = FamilyDarkOnErrorContainer
)

private val BentoLightColorScheme = lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    secondaryContainer = BentoSecondaryContainer,
    onSecondaryContainer = BentoOnSecondaryContainer,
    tertiary = BentoTertiary,
    onTertiary = BentoOnTertiary,
    tertiaryContainer = BentoTertiaryContainer,
    onTertiaryContainer = BentoOnTertiaryContainer,
    background = BentoBackground,
    onBackground = BentoOnBackground,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    surfaceContainerLow = BentoSurfaceContainerLow,
    surfaceContainer = BentoSurfaceContainer,
    outline = BentoOutline,
    outlineVariant = BentoOutlineVariant,
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

val BentoShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FamilyWallTheme(
    designStyle: AppDesignStyle = AppDesignStyle.EDITORIAL,
    darkMode: ThemeDarkMode = ThemeDarkMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (darkMode) {
        ThemeDarkMode.SYSTEM -> isSystemInDarkTheme()
        ThemeDarkMode.LIGHT -> false
        ThemeDarkMode.DARK -> true
    }

    val colorScheme = when (designStyle) {
        AppDesignStyle.EDITORIAL -> if (isDark) EditorialDarkColorScheme else EditorialLightColorScheme
        AppDesignStyle.NORDIC_BENTO -> if (isDark) BentoDarkColorScheme else BentoLightColorScheme
    }

    val typography = when (designStyle) {
        AppDesignStyle.EDITORIAL -> EditorialTypography
        AppDesignStyle.NORDIC_BENTO -> BentoTypography
    }

    val shapes = when (designStyle) {
        AppDesignStyle.EDITORIAL -> FamilyShapes
        AppDesignStyle.NORDIC_BENTO -> BentoShapes
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalDesignStyle provides designStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
