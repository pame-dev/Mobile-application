package com.pame.karsy.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyWhite
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
fun rememberUnreadNotifications(enabled: Boolean): Int {
    var sinLeer by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(enabled, AvisosNuevos.cambios) {
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

/** Círculo rojo con el número de avisos sin leer, en la esquina de la campana. */
@Composable
fun BoxScope.UnreadBadge(count: Int) {
    if (count <= 0) return
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 6.dp, y = (-6).dp)
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(KarsyError)
            .border(2.dp, KarsySurface, CircleShape)
            .padding(horizontal = 5.dp)
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            fontFamily = DmSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyWhite
        )
    }
}
