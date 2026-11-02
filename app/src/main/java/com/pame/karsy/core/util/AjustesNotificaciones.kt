package com.pame.karsy.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Abre los ajustes de notificaciones de Karsy en el teléfono. Esa pantalla existe desde Android 8;
 * antes (o si no abre) se muestra la información de la app, donde también están las notificaciones.
 */
fun abrirAjustesDeNotificaciones(context: Context) {
    val abrio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }.isSuccess
    if (!abrio) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }
    }
}
