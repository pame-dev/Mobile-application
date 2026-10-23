package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import java.text.Normalizer

private val SeminuevoBg = Color(0xFF1D3F40)
private val SeminuevoText = Color(0xFF6FC9C4)
private val UsadoBg = Color(0xFF3A3320)
private val UsadoText = Color(0xFFE8C66A)

/**
 * Etiqueta "Seminuevo" / "Usado" junto al nombre del vehículo. Si la condición viene vacía
 * o con otro valor no dibuja nada (ni deja espacio).
 */
@Composable
fun ConditionTag(condicion: String?, compact: Boolean = false, modifier: Modifier = Modifier) {
    val valor = normalizarCondicion(condicion) ?: return
    val seminuevo = valor == CONDICION_SEMINUEVO
    Text(
        stringResource(if (seminuevo) R.string.publish_condition_seminuevo else R.string.publish_condition_usado),
        fontFamily = DmSans,
        fontSize = if (compact) 11.sp else 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (seminuevo) SeminuevoText else UsadoText,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (seminuevo) SeminuevoBg else UsadoBg)
            .padding(
                horizontal = if (compact) 9.dp else 12.dp,
                vertical = if (compact) 3.dp else 5.dp
            )
    )
}

internal const val CONDICION_SEMINUEVO = "seminuevo"
internal const val CONDICION_USADO = "usado"

/** "Seminuevo", "USADO", "usádo"… → "seminuevo" / "usado"; cualquier otro valor → null. */
internal fun normalizarCondicion(condicion: String?): String? {
    if (condicion.isNullOrBlank()) return null
    val sinAcentos = Normalizer.normalize(condicion.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
    return sinAcentos.takeIf { it == CONDICION_SEMINUEVO || it == CONDICION_USADO }
}
