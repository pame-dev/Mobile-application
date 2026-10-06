package com.pame.karsy.core.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.pame.karsy.core.supabase.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Registro del teléfono para notificaciones push. La BD guarda el token de Firebase
 * de cada teléfono con su cuenta (registrar_dispositivo / quitar_dispositivo);
 * la Edge Function enviar-push lo usa para entregar cada aviso nuevo.
 *
 * Firebase entrega el token en [KarsyMessagingService.onRegistered]; se guarda aquí
 * para poder registrarlo al iniciar sesión y quitarlo al cerrarla.
 */
object PushTokens {
    private const val TAG = "KarsyPush"
    private const val PREFS = "push"
    private const val KEY_TOKEN = "token"

    /** Al iniciar o restaurar sesión: pide el token a Firebase y asocia el teléfono a la cuenta. */
    suspend fun register(context: Context) {
        // Con la API nueva el token llega a onRegistered; si ya teníamos uno, se registra de inmediato.
        FirebaseMessaging.getInstance().register()
        val token = savedToken(context) ?: legacyToken()
        if (token == null) {
            Log.w(TAG, "Firebase no entregó el token de este teléfono")
            return
        }
        onToken(context, token)
    }

    /**
     * Respaldo: no todas las versiones de Google Play Services llaman a onRegistered
     * (en las pruebas con el emulador no llegó), así que se pide el token a la forma anterior.
     */
    @Suppress("DEPRECATION")
    private suspend fun legacyToken(): String? = runCatching {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }.onFailure { Log.w(TAG, "No se pudo obtener el token de Firebase: ${describe(it)}") }.getOrNull()

    /** Firebase entregó (o cambió) el token del teléfono. */
    suspend fun onToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TOKEN, token).apply()
        if (Supabase.client.auth.currentSessionOrNull() == null) return
        runCatching { saveInDatabase(token) }
            .onFailure { Log.w(TAG, "No se pudo registrar el teléfono en la BD: ${describe(it)}") }
    }

    /**
     * Antes de cerrar sesión (después ya no hay permiso): el teléfono deja de recibir
     * avisos de esa cuenta.
     */
    suspend fun unregister(context: Context) {
        val token = savedToken(context) ?: return
        runCatching {
            Supabase.client.postgrest.rpc("quitar_dispositivo", buildJsonObject { put("p_token", token) })
        }
    }

    /**
     * Solo el tipo de error y su código: el mensaje completo de Supabase incluye los
     * encabezados de la petición (con el token de sesión) y no debe ir al log.
     */
    private fun describe(error: Throwable): String =
        (error as? PostgrestRestException)?.let { "${it.code}: ${it.error}" } ?: error.javaClass.simpleName

    private fun savedToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    private suspend fun saveInDatabase(token: String) {
        Supabase.client.postgrest.rpc("registrar_dispositivo", buildJsonObject { put("p_token", token) })
    }
}
