package com.pame.karsy.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.pame.karsy.data.model.Car

/**
 * Publicaciones rechazadas o deshabilitadas que el dueño ocultó de su perfil.
 * Solo se guarda en el teléfono (por cuenta); en Supabase la publicación no cambia.
 * Desde Mi panel se ven las ocultas y se pueden volver a mostrar.
 */
object PublicacionesOcultas {
    private const val PREFS = "karsy_prefs"
    private const val KEY_PREFIX = "ocultas_"

    /** Estados que se pueden ocultar; si la publicación vuelve a estar activa, aparece de nuevo. */
    private val ESTADOS_OCULTABLES = listOf("Rechazado", "Deshabilitado", "Pausado")

    private var prefs: SharedPreferences? = null

    /** Cambia al ocultar / mostrar para que las pantallas que leen [estaOculta] se recompongan. */
    private var version by mutableIntStateOf(0)

    /** Se llama en onCreate de la Activity. */
    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun sePuedeOcultar(car: Car): Boolean = car.status in ESTADOS_OCULTABLES

    fun estaOculta(car: Car): Boolean = sePuedeOcultar(car) && car.id in ids()

    fun ocultar(id: Long) = guardar(ids() + id)

    fun mostrar(id: Long) = guardar(ids() - id)

    private fun key(): String? = SessionManager.userId?.let { KEY_PREFIX + it }

    private fun ids(): Set<Long> {
        version
        val key = key() ?: return emptySet()
        return prefs?.getStringSet(key, null).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun guardar(ids: Set<Long>) {
        val key = key() ?: return
        prefs?.edit()?.putStringSet(key, ids.map { it.toString() }.toSet())?.apply()
        version++
    }
}
