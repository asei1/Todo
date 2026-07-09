package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SlateDarkPrimary,
    secondary = SlateDarkSecondary,
    tertiary = SlateDarkTertiary,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    onPrimary = SlateDarkOnPrimary,
    onSecondary = SlateDarkOnPrimary,
    onBackground = SlateDarkOnBackground,
    onSurface = SlateDarkOnSurface,
    surfaceVariant = SlateDarkSurface,
    onSurfaceVariant = SlateDarkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = SoftLightPrimary,
    secondary = SoftLightSecondary,
    tertiary = SoftLightTertiary,
    background = SoftLightBackground,
    surface = SoftLightSurface,
    onPrimary = SoftLightOnPrimary,
    onSecondary = SoftLightOnPrimary,
    onBackground = SoftLightOnBackground,
    onSurface = SoftLightOnSurface,
    surfaceVariant = SoftLightSurface,
    onSurfaceVariant = SoftLightOnSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
