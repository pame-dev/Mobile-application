package com.pame.karsy.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KarsyColorScheme = lightColorScheme(
    primary = KarsyNavy,
    secondary = KarsyTeal,
    background = KarsyBg,
    surface = KarsyWhite,
    onPrimary = KarsyWhite,
    onSecondary = KarsyWhite,
    onBackground = KarsyCharcoal,
    onSurface = KarsyCharcoal,
    outline = KarsyBorder
)

@Composable
fun KarsyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KarsyColorScheme,
        typography = KarsyTypography,
        content = content
    )
}