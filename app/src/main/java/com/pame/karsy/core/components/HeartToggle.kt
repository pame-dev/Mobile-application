package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.Tema

/** Rojo coral del corazón de favoritos guardado (igual en tema claro y oscuro). */
val HeartCoral = Color(0xFFE65B5B)

/**
 * Ícono de corazón: relleno rojo coral cuando está guardado; si no, contorno en
 * [outlineTint] (gris sobre fondos claros, blanco sobre fotos).
 */
@Composable
fun HeartIcon(filled: Boolean, modifier: Modifier = Modifier, size: Dp = 18.dp, outlineTint: Color = KarsyMid) {
    Icon(
        imageVector = if (filled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = stringResource(if (filled) R.string.core_remove_favorite else R.string.core_add_favorite),
        tint = if (filled) HeartCoral else outlineTint,
        modifier = modifier.size(size)
    )
}

/**
 * Botón circular con corazón para poner sobre las fotos de las tarjetas. Se lee igual
 * sobre fotos claras u oscuras y en ambos temas:
 * - Guardado: corazón coral sobre blanco con sombra (claro) o sobre azul muy oscuro
 *   translúcido con borde fino (oscuro).
 * - No guardado: contorno blanco sobre un círculo negro translúcido.
 */
@Composable
fun HeartToggle(
    filled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    val oscuro = Tema.oscuro
    val fondo = when {
        !filled -> Color.Black.copy(alpha = 0.4f)
        oscuro -> Color(0xFF0D1B2A).copy(alpha = 0.75f)
        else -> Color.White
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (filled && !oscuro) Modifier.shadow(4.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(fondo)
            .then(if (filled && oscuro) Modifier.border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape) else Modifier)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        HeartIcon(filled = filled, size = size * 0.5f, outlineTint = Color.White)
    }
}
