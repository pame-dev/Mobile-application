package com.pame.karsy.core.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration

/**
 * Tema de la app (claro u oscuro), elegido en Configuración y guardado en preferencias.
 * Si no se ha elegido, o se eligió [SISTEMA], se sigue el modo del teléfono.
 *
 * Los colores de Color.kt leen [oscuro]; se fija al crear la Activity (antes de
 * setContent) y al cambiarlo la Activity se recrea para que toda la UI lo tome.
 */
object Tema {
    const val CLARO = "claro"
    const val OSCURO = "oscuro"
    const val SISTEMA = "system"

    private const val PREFS = "karsy_prefs"
    private const val KEY = "tema"

    /** Tema en uso en esta Activity. */
    var oscuro: Boolean = false
        private set

    /** Lo que eligió el usuario: [CLARO], [OSCURO] o [SISTEMA] (también si nunca eligió). */
    fun elegido(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: SISTEMA

    /** true si con lo elegido corresponde el tema oscuro. */
    fun esOscuro(context: Context): Boolean = when (elegido(context)) {
        OSCURO -> true
        CLARO -> false
        else -> telefonoEnOscuro(context)
    }

    fun telefonoEnOscuro(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /** Se llama en onCreate, antes de setContent. */
    fun aplicar(context: Context) {
        oscuro = esOscuro(context)
    }

    /** Guarda el tema y recrea la Activity si cambia el que está en uso. */
    fun cambiar(activity: Activity, codigo: String) {
        if (codigo == elegido(activity)) return
        val antes = esOscuro(activity)
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, codigo).apply()
        if (esOscuro(activity) != antes) activity.recreate()
    }
}
