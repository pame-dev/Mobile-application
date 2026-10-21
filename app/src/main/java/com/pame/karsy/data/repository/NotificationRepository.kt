package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionManager
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import com.pame.karsy.data.remote.PreferenciasAvisosDto
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.remote.NotificacionDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order

/**
 * Bandeja de notificaciones del usuario (Configuración → Notificaciones).
 * Los avisos los crean triggers en la BD; la app solo los lee y los marca como leídos.
 */
object NotificationRepository {

    private val db get() = Supabase.client

    /** Avisos de la cuenta en sesión, del más nuevo al más viejo. */
    suspend fun mine(): List<NotificacionDto> {
        val uid = SessionManager.userId ?: return emptyList()
        return db.from("notificaciones").select {
            filter { eq("id_cuenta", uid) }
            order("fecha_creacion", Order.DESCENDING)
            limit(100)
        }.decodeList<NotificacionDto>()
    }

    /** Cuántos avisos sin leer tiene la cuenta en sesión (solo cuenta, no los descarga). */
    suspend fun unreadCount(): Int {
        val uid = SessionManager.userId ?: return 0
        return db.from("notificaciones").select(Columns.list("id_notificacion")) {
            head = true
            count(Count.EXACT)
            filter {
                eq("id_cuenta", uid)
                eq("leida", false)
            }
        }.countOrNull()?.toInt() ?: 0
    }

    /** Marca como leídos todos los avisos pendientes de la cuenta en sesión. */
    suspend fun markAllRead() {
        val uid = SessionManager.userId ?: return
        db.from("notificaciones").update({ set("leida", true) }) {
            filter {
                eq("id_cuenta", uid)
                eq("leida", false)
            }
        }
    }

    /** Qué avisos opcionales recibe la cuenta (sin fila guardada = todos). */
    suspend fun preferences(): PreferenciasAvisosDto {
        val uid = SessionManager.userId ?: return PreferenciasAvisosDto()
        return db.from("preferencias_notificacion").select { filter { eq("id_cuenta", uid) } }
            .decodeSingleOrNull<PreferenciasAvisosDto>() ?: PreferenciasAvisosDto()
    }

    suspend fun savePreferences(p: PreferenciasAvisosDto) {
        val uid = SessionManager.userId ?: return
        db.from("preferencias_notificacion").upsert(buildJsonObject {
            put("id_cuenta", uid)
            put("precios", p.precios)
            put("resumenes", p.resumenes)
            put("recomendaciones", p.recomendaciones)
            put("vendedor", p.vendedor)
            // Sin java.time: Instant necesita Android 8 y la app funciona desde Android 7.
            put("fecha_actualizacion", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date()))
        })
    }
}
