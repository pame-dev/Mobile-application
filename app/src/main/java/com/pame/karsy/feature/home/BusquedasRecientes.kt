package com.pame.karsy.feature.home

import android.content.Context
import androidx.core.content.edit

/**
 * Búsquedas recientes del buscador de Inicio, en este teléfono (las últimas primero). Son de
 * cada cuenta (o del visitante): en un teléfono compartido, otra cuenta no ve las de esta.
 * Igual que las recientes del buscador de la web, que se guardan en ese navegador.
 */
object BusquedasRecientes {
    private const val PREFS = "karsy_prefs"
    private const val CLAVE = "busquedas_recientes"
    private const val MAX = 6

    private fun clave(cuenta: String?) = "${CLAVE}_${cuenta ?: "visitante"}"

    fun leer(context: Context, cuenta: String?): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(clave(cuenta), null)
            ?.split('\n')?.filter { it.isNotBlank() }?.take(MAX).orEmpty()

    /** Guarda [texto] al principio (sin repetir) y devuelve la lista nueva. */
    fun guardar(context: Context, cuenta: String?, texto: String): List<String> {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return leer(context, cuenta)
        val lista = (listOf(limpio) + leer(context, cuenta).filterNot { it.equals(limpio, ignoreCase = true) }).take(MAX)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(clave(cuenta), lista.joinToString("\n")) }
        return lista
    }
}
