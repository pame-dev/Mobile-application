package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyVerifiedBlue

/** Azul de la insignia (fondo de la píldora y palomita). */
private val VerifiedBlue = KarsyVerifiedBlue

/** Escudo relleno blanco con palomita azul (mismo dibujo que .verified-badge de la web). */
private val EscudoVerificado: ImageVector = ImageVector.Builder(
    name = "EscudoVerificado",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = addPathNodes("M12 2l8 3v6c0 5-3.4 9.4-8 11-4.6-1.6-8-6-8-11V5z"),
        fill = SolidColor(Color.White),
    )
    addPath(
        pathData = addPathNodes("M8 12l3 3 5-5.5"),
        stroke = SolidColor(VerifiedBlue),
        strokeLineWidth = 2.6f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}.build()

/**
 * Insignia "Verificado": píldora azul con escudo blanco y texto blanco, a la derecha del nombre
 * de un usuario verificado (cuentas.verificado = true). Sirve igual para lotes y particulares.
 * [compact] = versión chica para las tarjetas de la lista de lotes.
 */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(VerifiedBlue)
            .padding(
                if (compact) PaddingValues(start = 5.dp, end = 9.dp, top = 3.dp, bottom = 3.dp)
                else PaddingValues(start = 7.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
            ),
    ) {
        // El ícono lleva sus propios colores (blanco y azul), por eso Image y no Icon con tint.
        Image(EscudoVerificado, contentDescription = stringResource(R.string.core_verified_badge_a11y), modifier = Modifier.size(if (compact) 13.dp else 16.dp))
        Text(
            stringResource(R.string.core_verified_badge),
            fontFamily = DmSans,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
        )
    }
}

/** Solo el escudo de la insignia, sin la palabra "Verificado": círculo azul (lista de Usuarios del admin). */
@Composable
fun VerifiedShield(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(VerifiedBlue),
    ) {
        Image(EscudoVerificado, contentDescription = stringResource(R.string.core_verified_badge_a11y), modifier = Modifier.size(14.dp))
    }
}
