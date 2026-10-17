package com.pame.karsy.core.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pame.karsy.core.util.Traductor

/**
 * Texto escrito por los usuarios o el admin (descripción de un auto en su tarjeta, de un
 * perfil o lote, motivos): en inglés se muestra traducido automáticamente (Traductor);
 * mientras llega o si falla, el original. Si ya estaba guardado se muestra desde el inicio.
 * Con [corto] (tarjetas y listas) solo se traduce el inicio, para gastar poco del límite
 * diario. Igual que Traducido en la web.
 */
@Composable
fun rememberTraduccion(texto: String, corto: Boolean = false): String {
    val enIngles = Idioma.actual(LocalContext.current) == Idioma.EN
    var traducido by remember(texto, corto, enIngles) {
        mutableStateOf(if (enIngles) Traductor.guardada(texto, corto) else null)
    }
    LaunchedEffect(texto, corto, enIngles) {
        if (enIngles && traducido == null && texto.isNotBlank()) traducido = Traductor.alIngles(texto, corto)
    }
    return traducido ?: texto
}
