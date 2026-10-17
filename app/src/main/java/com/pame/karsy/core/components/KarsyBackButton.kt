package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface

/**
 * Botón "‹" para regresar: cuadro redondeado con borde, igual en todas las pantallas
 * (y igual al VolverButton de la web).
 */
@Composable
fun KarsyBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.core_back),
) {
    val forma = RoundedCornerShape(12.dp)
    Box(
        // Área táctil de 48dp (recomendación de Android); el cuadro se dibuja de 40dp.
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(forma)
            .background(KarsySurface)
            .border(1.dp, KarsyBorder, forma)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.ChevronLeft,
            contentDescription = contentDescription,
            tint = KarsyInk,
            modifier = Modifier.size(24.dp)
        )
    }
}
