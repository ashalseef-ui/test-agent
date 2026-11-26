package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = StudioPrimary,
    secondary = StudioSecondary,
    background = StudioBackgroundDark,
    surface = StudioSurfaceDark,
    surfaceVariant = StudioSurfaceVariantDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = StudioTextPrimaryDark,
    onSurface = StudioTextPrimaryDark,
    onSurfaceVariant = StudioTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = StudioPrimary,
    secondary = StudioSecondary,
    background = StudioBackgroundLight,
    surface = StudioSurfaceLight,
    surfaceVariant = StudioSurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = StudioTextPrimaryLight,
    onSurface = StudioTextPrimaryLight,
    onSurfaceVariant = StudioTextSecondaryLight
)

@Composable
fun StudioTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
