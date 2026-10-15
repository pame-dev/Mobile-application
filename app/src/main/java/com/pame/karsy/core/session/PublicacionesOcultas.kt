package com.pame.karsy.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.model.Car
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Publicaciones rechazadas, deshabilitadas o pausadas que el dueño ocultó de su perfil.
 * Se guardan en la BD (tabla publicaciones_ocultas), así que la app y la web ven la misma
 * lista. La publicación no cambia: solo cómo la ve su dueño. Desde Mi panel se ven las
 * ocultas y se pueden volver a mostrar.
 */
object PublicacionesOcultas {
    /** Versión anterior: las ocultas se guardaban solo en el teléfono (se suben a la BD una vez). */
    private const val PREFS = "karsy_prefs"
    private const val KEY_PREFIX = "ocultas_"

    /** Estados que se pueden ocultar; si la publicación vuelve a estar activa, aparece de nuevo. */
    private val ESTADOS_OCULTABLES = listOf("Rechazado", "Deshabilitado", "Pausado")

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ids ocultos de la cuenta en sesión; al cambiar, las pantallas que leen [estaOculta] se recomponen. */
    private var ids by mutableStateOf<Set<Long>>(emptySet())
    /** Cuenta cuya lista está en [ids] (al cambiar de cuenta se vuelve a leer). */
    private var cuentaCargada: String? = null

    @Serializable
    private data class OcultaDto(@SerialName("id_publicacion") val idPublicacion: Long)

    /** Se llama en onCreate de la Activity. */
    fun iniciar(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun sePuedeOcultar(car: Car): Boolean = car.status in ESTADOS_OCULTABLES

    fun estaOculta(car: Car): Boolean {
        asegurarCargadas()
        return sePuedeOcultar(car) && car.id in ids
    }

    fun ocultar(id: Long) = cambiar(id, ocultar = true)

    fun mostrar(id: Long) = cambiar(id, ocultar = false)

    /**
     * Vuelve a leer la lista de la BD (al cargar el perfil y Mi panel). La primera vez sube
     * las que estaban guardadas solo en este teléfono.
     */
    suspend fun sincronizar() {
        val uid = SessionManager.userId ?: run {
            ids = emptySet()
            cuentaCargada = null
            return
        }
        val db = Supabase.client
        val remotas = db.from("publicaciones_ocultas")
            .select(Columns.list("id_publicacion")) { filter { eq("id_cuenta", uid) } }
            .decodeList<OcultaDto>()
            .map { it.idPublicacion }
            .toMutableSet()

        val key = KEY_PREFIX + uid
        val locales = prefs?.getStringSet(key, null).orEmpty().mapNotNull { it.toLongOrNull() }
        for (id in locales - remotas) {
            // Si ya no es suya o se borró, la BD la rechaza y simplemente no se sube.
            runCatching { insertar(uid, id) }.onSuccess { remotas += id }
        }
        if (locales.isNotEmpty()) prefs?.edit()?.remove(key)?.apply()

        ids = remotas
        cuentaCargada = uid
    }

    /** Si la lista en memoria es de otra cuenta (o de ninguna), se pide la de la cuenta actual. */
    private fun asegurarCargadas() {
        val uid = SessionManager.userId
        if (uid == cuentaCargada) return
        cuentaCargada = uid
        ids = emptySet()
        if (uid != null) scope.launch { runCatching { sincronizar() } }
    }

    /** Cambio inmediato en pantalla; si la BD falla se revierte. */
    private fun cambiar(id: Long, ocultar: Boolean) {
        val uid = SessionManager.userId ?: return
        val antes = ids
        ids = if (ocultar) ids + id else ids - id
        scope.launch {
            runCatching {
                if (ocultar) insertar(uid, id)
                else Supabase.client.from("publicaciones_ocultas").delete {
                    filter {
                        eq("id_cuenta", uid)
                        eq("id_publicacion", id)
                    }
                }
            }.onFailure { ids = antes }
        }
    }

    private suspend fun insertar(uid: String, id: Long) {
        Supabase.client.from("publicaciones_ocultas").upsert(
            buildJsonObject {
                put("id_cuenta", uid)
                put("id_publicacion", id)
            }
        ) { ignoreDuplicates = true }
    }
}
