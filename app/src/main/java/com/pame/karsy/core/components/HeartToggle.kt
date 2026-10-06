package com.pame.karsy.core.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite

/** Ícono de corazón: relleno teal cuando está activo, contorno gris si no. */
@Composable
fun HeartIcon(filled: Boolean, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Icon(
        imageVector = if (filled) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = stringResource(if (filled) R.string.core_remove_favorite else R.string.core_add_favorite),
        tint = if (filled) KarsyTeal else KarsyMid,
        modifier = modifier.size(size)
    )
}

/** Botón circular blanco con corazón, para poner sobre las fotos de las tarjetas. */
@Composable
fun HeartToggle(
    filled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(KarsySurface)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        HeartIcon(filled = filled, size = size * 0.5f)
    }
}
