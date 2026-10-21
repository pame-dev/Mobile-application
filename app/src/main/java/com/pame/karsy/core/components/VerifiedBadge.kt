package com.pame.karsy.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Azul de la insignia (ícono y texto). */
private val VerifiedBlue = Color(0xFF7FB2E5)

/** Escudo con contorno y palomita (mismo trazo que .verified-badge de la web). */
private val EscudoVerificado: ImageVector = ImageVector.Builder(
    name = "EscudoVerificado",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = addPathNodes("M12 2l8 3v6c0 5-3.4 9.4-8 11-4.6-1.6-8-6-8-11V5z"),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineJoin = StrokeJoin.Round,
    )
    addPath(
        pathData = addPathNodes("M8 12l3 3 5-5.5"),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2.3f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}.build()

/**
 * Insignia "Verificado" (escudo + texto, sin fondo) a la derecha del nombre. Solo se muestra
 * cuando el admin aprobó el documento de verificación (cuentas.verificado = true). Sirve igual
 * para lotes y particulares.
 */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Icon(
            EscudoVerificado,
            contentDescription = stringResource(R.string.core_verified_badge_a11y),
            tint = VerifiedBlue,
            modifier = Modifier.size(17.dp),
        )
        Text(
            stringResource(R.string.core_verified_badge),
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = VerifiedBlue,
            maxLines = 1,
        )
    }
}
