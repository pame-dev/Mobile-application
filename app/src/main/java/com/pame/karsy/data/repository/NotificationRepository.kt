package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.remote.NotificacionDto
import io.github.jan.supabase.postgrest.from
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
}
