package com.pame.karsy.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.NotificationRepository

/** Cada cuánto se vuelve a contar mientras la pantalla está visible. */
private const val CADA_CUANTO_MS = 60_000L

/** Avisa que llegó un push, para que las campanas visibles vuelvan a contar los avisos sin leer. */
object AvisosNuevos {
    var cambios by mutableIntStateOf(0)
        private set

    fun avisar() {
        cambios++
    }
}

/**
 * Avisos sin leer de la cuenta en sesión (número de la campana, como en la web). Se vuelve
 * a contar al volver a la pantalla (p. ej. después de abrir Notificaciones), cada minuto mientras
 * se ve y al llegar un push.
 */
@Composable
fun rememberUnreadNotifications(
    enabled: Boolean,
    /** Al cambiar, se vuelve a contar (la barra inferior no se recrea al cambiar de pantalla). */
    refresco: Any? = null,
): Int {
    var sinLeer by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(enabled, AvisosNuevos.cambios, refresco) {
        if (!enabled) {
            sinLeer = 0
            return@LaunchedEffect
        }
        // Mientras la pantalla está visible: al quedar visible y luego cada minuto (así el número
        // cambia solo, como en la web). Al salir de la pantalla se detiene. Sin mensaje de error:
        // si falla, la campana se queda con el número anterior.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                safeCall { NotificationRepository.unreadCount() }.onSuccess { sinLeer = it }
                delay(CADA_CUANTO_MS)
            }
        }
    }
    return sinLeer
}
