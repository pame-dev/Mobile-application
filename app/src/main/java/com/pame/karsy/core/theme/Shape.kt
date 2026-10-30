package com.pame.karsy.core.theme

import androidx.compose.ui.unit.dp

/**
 * Radios de esquina de la app. Usar `RoundedCornerShape(KarsyRadius.X)` en lugar de escribir
 * `N.dp` a mano; para píldoras y círculos, `CircleShape`.
 */
object KarsyRadius {
    /** Casillas, barras y chips muy pequeños. */
    val Xs = 6.dp
    val Sm = 8.dp
    /** Botones, campos y chips. */
    val Md = 12.dp
    /** Tarjetas. */
    val Lg = 16.dp
    /** Diálogos y hojas inferiores. */
    val Xl = 20.dp
    /** Elementos flotantes grandes (barra inferior, modales destacados). */
    val Xxl = 28.dp
}
