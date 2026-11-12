package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSize

/**
 * Mientras se guarda o envía algo: ventana encima de todo (también de hojas y diálogos) que
 * oscurece la pantalla, muestra que está cargando y no deja tocar nada más ni regresar
 * (tocar otra cosa a medio guardar causaba errores). Se pone en cualquier parte de la
 * pantalla con [visible] = la acción en curso.
 */
@Composable
fun CargandoBloqueo(visible: Boolean, texto: String? = null) {
    if (!visible) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .padding(32.dp)
                    .widthIn(max = 300.dp)
                    .background(KarsySurface, RoundedCornerShape(KarsyRadius.Lg))
                    .padding(horizontal = 28.dp, vertical = 24.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(color = KarsyTeal, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                Text(
                    texto ?: stringResource(R.string.core_cargando_bloqueo),
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Body,
                    fontWeight = FontWeight.Medium,
                    color = KarsyInk,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
