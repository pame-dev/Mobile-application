package com.pame.karsy.core.theme

import androidx.compose.ui.graphics.Color

// Paleta tomada de los mockups (Muckups/Karsy Mobile App UI Design), con su versión oscura.
// Cada color devuelve el valor del tema en uso (Tema.oscuro); se fija al crear la Activity.

private fun tema(claro: Long, oscuro: Long): Color = Color(if (Tema.oscuro) oscuro else claro)

/** Azul de la marca para fondos (botones, chips activos). En oscuro, un poco más claro para destacar. */
val KarsyNavy: Color get() = tema(0xFF0D2B45, 0xFF2D6390)
/** Azul marino fijo de la marca (banners, degradados): igual en claro y oscuro. */
val KarsyNavyDeep = Color(0xFF0D2B45)
val KarsyNavyLight: Color get() = tema(0xFF1A4A6E, 0xFF3D78A8)
/** Títulos y textos fuertes: azul marino en claro, casi blanco en oscuro. */
val KarsyInk: Color get() = tema(0xFF0D2B45, 0xFFE6EDF3)
val KarsyTeal = Color(0xFF52A3AA)
val KarsyTealLight: Color get() = tema(0xFFEBF6F7, 0xFF16353A)
val KarsyTealSoft: Color get() = tema(0xFFE8F4F5, 0xFF15312F)
/** Fondo de las pantallas: gris azulado en claro (para que las tarjetas se distingan), casi negro en oscuro. */
val KarsyBg: Color get() = tema(0xFFEAEFF4, 0xFF0F151B)
/** Fondo de tarjetas, hojas y campos (blanco en claro). */
val KarsySurface: Color get() = tema(0xFFFFFFFF, 0xFF1A232C)
/** Borde de las tarjetas sobre [KarsyBg]. */
val KarsyCardOutline: Color get() = tema(0xFFD9E1E8, 0xFF2A3540)
val KarsyCharcoal: Color get() = tema(0xFF333333, 0xFFE2E7EC)
val KarsyMid: Color get() = tema(0xFF8E9A8E, 0xFF97A3AE)
val KarsyTextSecondary: Color get() = tema(0xFF667085, 0xFFA6B1BC)
/** Blanco fijo: texto e íconos sobre botones de color. Para fondos de tarjeta usar [KarsySurface]. */
val KarsyWhite = Color(0xFFFFFFFF)
val KarsyBorder: Color get() = tema(0xFFE2E8ED, 0xFF2C3844)
val KarsyBorderMuted: Color get() = tema(0xFFD4DDE4, 0xFF3A4652)
val KarsyCheckBorder: Color get() = tema(0xFFC4CCCC, 0xFF4B5763)
val KarsyDisabled: Color get() = tema(0xFFD0D8E0, 0xFF3A4652)

// Estados
val KarsySuccess = Color(0xFF2E7D32)
val KarsySuccessLight: Color get() = tema(0xFFE8F5E9, 0xFF15301A)
val KarsyError = Color(0xFFD32F2F)
val KarsyErrorLight: Color get() = tema(0xFFFDECEA, 0xFF3B1919)
val KarsyWarning = Color(0xFFF59E0B)
val KarsyWarningLight: Color get() = tema(0xFFFFF7E6, 0xFF3A2D12)
val KarsyStar = Color(0xFFF5B400)

/**
 * Fondo suave de un color de acento (avisos, badges, notificaciones): el pastel [claro]
 * en tema claro y el [acento] translúcido en oscuro, que sobre el fondo queda apagado.
 */
fun karsyTint(acento: Color, claro: Color): Color = if (Tema.oscuro) acento.copy(alpha = 0.18f) else claro

/** Fondo de las fotos mientras cargan. */
val KarsyImagePlaceholder: Color get() = tema(0xFFDDE6EC, 0xFF26313B)
