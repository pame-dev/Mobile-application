package com.pame.karsy.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.Outfit

/**
 * Botón X de los avisos (esquina superior derecha). Con [enabled] = false (p. ej. mientras
 * guarda) no cierra, igual que tocar fuera del aviso.
 */
@Composable
fun BotonCerrarAviso(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(40.dp)) {
        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.core_close), tint = KarsyMid, modifier = Modifier.size(20.dp))
    }
}

/**
 * Título de un AlertDialog: centrado y con la X arriba a la derecha (igual que los avisos de
 * la web). La X se corre hacia la esquina del aviso para no empujar el título.
 */
@Composable
fun TituloAviso(texto: String, onClose: () -> Unit, closeEnabled: Boolean = true, fontSize: TextUnit = TextUnit.Unspecified) {
    Box(Modifier.fillMaxWidth()) {
        Text(
            texto,
            fontFamily = Outfit,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = KarsyInk,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .align(Alignment.Center)
        )
        BotonCerrarAviso(
            onClick = onClose,
            enabled = closeEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-14).dp)
        )
    }
}

/** Texto principal de un AlertDialog, centrado. */
@Composable
fun TextoAviso(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        fontFamily = DmSans,
        fontSize = KarsyTextSize.BodyLarge,
        color = KarsyCharcoal,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}
