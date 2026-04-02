package com.redn.farm.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = FarmGreen,
    onPrimary = SurfaceLight,
    primaryContainer = FarmGreenLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = SageMedium,
    onSecondary = SurfaceLight,
    secondaryContainer = SageLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = EarthTaupe,
    onTertiary = SurfaceLight,
    tertiaryContainer = EarthBeige,
    onTertiaryContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    error = FarmError,
    onError = FarmOnError,
    errorContainer = FarmErrorContainer,
    onErrorContainer = FarmOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = FarmGreenLight,
    onPrimary = TextPrimaryDark,
    primaryContainer = FarmGreen,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SageLight,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SageDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = EarthBeige,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = EarthTaupe,
    onTertiaryContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = BackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    error = FarmError,
    onError = FarmOnError,
    errorContainer = FarmErrorContainer,
    onErrorContainer = FarmOnErrorContainer
)

@Composable
fun FarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to use our custom colors
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}