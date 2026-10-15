package com.pame.karsy.core.push

import androidx.annotation.StringRes
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import com.pame.karsy.core.util.Formato
import android.content.Context
import com.pame.karsy.R

/**
 * Texto de cada tipo de aviso de la tabla `notificaciones`. Lo usan la bandeja
 * (NotificationsScreen) y las notificaciones push, para que digan lo mismo.
 * Cada texto recibe el título del vehículo como %1$s.
 */
@StringRes
fun notificationTextRes(tipo: String): Int = when (tipo) {
    "favorito" -> R.string.notif_favorite
    "propuesta_enviada" -> R.string.notif_sent
    "publicacion_aprobada" -> R.string.notif_approved
    "destacado_aprobado" -> R.string.notif_featured_mine
    "edicion_aprobada" -> R.string.notif_edit_approved
    "publicacion_rehabilitada" -> R.string.notif_reenabled
    "publicacion_rechazada" -> R.string.notif_rejected
    "edicion_rechazada" -> R.string.notif_edit_rejected
    "publicacion_deshabilitada" -> R.string.notif_disabled
    "destacado_rechazado" -> R.string.notif_featured_rejected
    "destacado_por_vencer" -> R.string.notif_featured_expiring
    "cuenta_suspendida" -> R.string.notif_account_suspended
    "cuenta_reactivada" -> R.string.notif_account_reactivated
    "reporte_atendido" -> R.string.notif_report_resolved
    "reporte_descartado" -> R.string.notif_report_dismissed
    "busqueda_coincidencia" -> R.string.notif_saved_search_match
    "favorito_no_disponible" -> R.string.notif_favorite_unavailable
    "auto_parecido" -> R.string.notif_similar_car
    "poca_actividad" -> R.string.notif_low_activity
    "confirmar_venta" -> R.string.notif_confirm_sale
    else -> R.string.notif_generic
}

/**
 * Texto completo del aviso. Los que llevan números (precio, vistas, autos nuevos) los
 * leen de [datos] (columna notificaciones.datos); si no vienen, se usa una versión sin
 * números. Lo usan la bandeja y las notificaciones push.
 */
fun notificationText(context: Context, tipo: String, vehiculo: String, datos: Map<String, String> = emptyMap()): String {
    fun numero(clave: String) = datos[clave]?.toDoubleOrNull()?.toInt()
    fun precio(clave: String) = datos[clave]?.toDoubleOrNull()?.let(Formato::precio)
    return when (tipo) {
        "precio_bajo" -> {
            val ahora = precio("precio")
            val antes = precio("precio_anterior")
            if (ahora != null && antes != null) context.getString(R.string.notif_price_drop, vehiculo, ahora, antes)
            else context.getString(R.string.notif_price_drop_simple, vehiculo)
        }
        "resumen_semanal" -> {
            val nuevos = numero("nuevos")
            val estado = datos["estado"]
            val coinciden = numero("coincidencias") ?: 0
            when {
                nuevos == null || estado == null -> context.getString(R.string.notif_weekly_summary_simple)
                coinciden > 0 -> context.getString(R.string.notif_weekly_summary_matches, estado, nuevos, coinciden)
                else -> context.getString(R.string.notif_weekly_summary, estado, nuevos)
            }
        }
        "resumen_vendedor" -> {
            val vistas = numero("vistas")
            val favoritos = numero("favoritos")
            if (vistas != null && favoritos != null) context.getString(R.string.notif_seller_summary, vistas, favoritos)
            else context.getString(R.string.notif_seller_summary_simple)
        }
        "hito_vistas" -> numero("vistas")?.let { context.getString(R.string.notif_views_milestone, vehiculo, it) }
            ?: context.getString(R.string.notif_views_milestone_simple, vehiculo)
        else -> context.getString(notificationTextRes(tipo), vehiculo)
    }
}

/** notificaciones.datos (JSON) → mapa de textos: {"vistas": 50} → {"vistas" to "50"}. */
fun datosDeAviso(json: JsonObject?): Map<String, String> =
    json?.mapValues { (_, v) -> (v as? JsonPrimitive)?.content.orEmpty() }.orEmpty()

/** Igual, desde el texto JSON que llega en el push. */
fun datosDeAviso(json: String?): Map<String, String> =
    json?.takeIf { it.isNotBlank() }?.let { runCatching { datosDeAviso(Json.parseToJsonElement(it).jsonObject) }.getOrNull() }.orEmpty()
