package com.example.passagenexpress.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val TotemLightColors = lightColorScheme(
    primary = TotemPalette.Accent,
    onPrimary = TotemPalette.Paper,
    primaryContainer = TotemPalette.Primary100,
    onPrimaryContainer = TotemPalette.AccentDeep,
    secondary = TotemPalette.Ink,
    onSecondary = TotemPalette.Paper,
    secondaryContainer = TotemPalette.PaperDim,
    onSecondaryContainer = TotemPalette.Ink,
    tertiary = TotemPalette.AccentDeep,
    onTertiary = TotemPalette.Paper,
    background = TotemPalette.Paper,
    onBackground = TotemPalette.Ink,
    surface = TotemPalette.Paper,
    onSurface = TotemPalette.Ink,
    surfaceVariant = TotemPalette.PaperDim,
    onSurfaceVariant = TotemPalette.InkMuted,
    outline = TotemPalette.Hairline,
    outlineVariant = TotemPalette.Hairline,
    error = TotemPalette.Error,
    onError = TotemPalette.Paper,
    errorContainer = TotemPalette.ErrorLight,
    onErrorContainer = TotemPalette.Error,
)

@Composable
fun TotemTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTotemDimens provides TotemDimens(),
        LocalTotemMotion provides TotemMotion(),
    ) {
        MaterialTheme(
            colorScheme = TotemLightColors,
            typography = TotemTypography,
            shapes = TotemShapes,
            content = content,
        )
    }
}

object TotemTheme {
    val dimens: TotemDimens
        @Composable get() = LocalTotemDimens.current

    val motion: TotemMotion
        @Composable get() = LocalTotemMotion.current
}
