package com.pame.karsy.core.push

import androidx.annotation.StringRes
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
    "reporte_atendido" -> R.string.notif_report_resolved
    "reporte_descartado" -> R.string.notif_report_dismissed
    else -> R.string.notif_generic
}
