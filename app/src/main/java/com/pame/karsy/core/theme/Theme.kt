package com.pame.karsy.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Colores de los componentes de Material (diálogos, hojas, menús) según el tema en uso. */
private fun karsyColorScheme() = if (Tema.oscuro) {
    darkColorScheme(
        primary = KarsyNavy,
        secondary = KarsyTeal,
        background = KarsyBg,
        surface = KarsySurface,
        surfaceContainer = KarsySurface,
        surfaceContainerLow = KarsySurface,
        surfaceContainerHigh = KarsySurface,
        onPrimary = KarsyWhite,
        onSecondary = KarsyWhite,
        onBackground = KarsyCharcoal,
        onSurface = KarsyCharcoal,
        onSurfaceVariant = KarsyTextSecondary,
        outline = KarsyBorder
    )
} else {
    lightColorScheme(
        primary = KarsyNavy,
        secondary = KarsyTeal,
        background = KarsyBg,
        surface = KarsySurface,
        onPrimary = KarsyWhite,
        onSecondary = KarsyWhite,
        onBackground = KarsyCharcoal,
        onSurface = KarsyCharcoal,
        outline = KarsyBorder
    )
}

@Composable
fun KarsyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = karsyColorScheme(),
        typography = KarsyTypography,
        content = content
    )
}
