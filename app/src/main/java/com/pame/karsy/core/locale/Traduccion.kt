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
 * Texto escrito por los usuarios (descripción de un perfil o lote, motivo de un reporte): en
 * inglés se muestra traducido automáticamente (Traductor); mientras llega o si falla, el
 * original. Si ya estaba guardado se muestra desde el inicio. Igual que Traducido en la web.
 */
@Composable
fun rememberTraduccion(texto: String): String {
    val enIngles = Idioma.actual(LocalContext.current) == Idioma.EN
    var traducido by remember(texto, enIngles) {
        mutableStateOf(if (enIngles) Traductor.guardada(texto) else null)
    }
    LaunchedEffect(texto, enIngles) {
        if (enIngles && traducido == null && texto.isNotBlank()) traducido = Traductor.alIngles(texto)
    }
    return traducido ?: texto
}
