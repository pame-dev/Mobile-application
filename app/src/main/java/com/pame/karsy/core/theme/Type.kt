package com.pame.karsy.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pame.karsy.R

/**
 * Escala de tamaños de letra de la app. Usar siempre estos valores en lugar de escribir
 * `N.sp` a mano, para que los textos se vean parejos entre pantallas.
 */
object KarsyTextSize {
    val Tiny = 10.sp
    val Caption = 11.sp
    val Small = 12.sp
    val Body = 13.sp
    val BodyLarge = 14.sp
    val Subtitle = 15.sp
    val Title = 16.sp
    val TitleLarge = 18.sp
    val Headline = 20.sp
    val HeadlineLarge = 22.sp
    val Display = 24.sp
    val DisplayLarge = 28.sp
    val Hero = 32.sp
}

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold)
)

val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
    Font(R.font.dmsans_bold, FontWeight.Bold)
)

val KarsyTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = KarsyTextSize.DisplayLarge
    ),
    titleLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.SemiBold,
        fontSize = KarsyTextSize.Headline
    ),
    titleMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = KarsyTextSize.Title
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = KarsyTextSize.Subtitle
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Normal,
        fontSize = KarsyTextSize.Body
    ),
    labelLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = KarsyTextSize.Body
    )
)