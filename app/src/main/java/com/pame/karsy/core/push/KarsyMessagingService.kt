package com.pame.karsy.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pame.karsy.MainActivity
import com.pame.karsy.R
import com.pame.karsy.core.locale.Idioma
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Recibe los avisos que envía la Edge Function enviar-push. Llegan solo con datos
 * (tipo, vehículo, motivo) y el texto se arma aquí, en el idioma elegido en la app,
 * con los mismos textos que la bandeja de notificaciones.
 */
class KarsyMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRegistered(token: String) {
        // Firebase entregó o cambió el token: se guarda y, si hay sesión, se actualiza en la BD.
        scope.launch { runCatching { PushTokens.onToken(applicationContext, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val tipo = data["tipo"] ?: return
        show(
            context = this,
            id = data["id_notificacion"]?.toIntOrNull() ?: tipo.hashCode(),
            tipo = tipo,
            vehiculo = data["titulo_vehiculo"],
            motivo = data["motivo"],
        )
    }

    companion object {
        const val CHANNEL_ID = "avisos"
        /** Extra del intent: al tocar la notificación se abre la bandeja de avisos. */
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"

        /** Canal de Android para los avisos de la cuenta (obligatorio desde Android 8). */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val local = Idioma.envolver(context)
            val channel = NotificationChannel(
                CHANNEL_ID,
                local.getString(R.string.push_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = local.getString(R.string.push_channel_desc) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun show(context: Context, id: Int, tipo: String, vehiculo: String?, motivo: String?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return
            createChannel(context)

            val local = Idioma.envolver(context)
            val nombre = vehiculo?.takeIf { it.isNotBlank() } ?: local.getString(R.string.notif_vehicle_fallback)
            val texto = local.getString(notificationTextRes(tipo), nombre)
            val completo = motivo?.takeIf { it.isNotBlank() }
                ?.let { "$texto\n${local.getString(R.string.notif_reason, it)}" } ?: texto

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            }
            val pending = PendingIntent.getActivity(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setColor(ContextCompat.getColor(context, R.color.ic_launcher_background))
                .setContentTitle(local.getString(R.string.app_name_display))
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(completo))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
        }
    }
}
