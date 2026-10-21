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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.NotificationRepository

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
 * a contar al volver a la pantalla (p. ej. después de abrir Notificaciones) y al llegar un push.
 */
@Composable
fun rememberUnreadNotifications(enabled: Boolean): Int {
    var sinLeer by remember { mutableIntStateOf(0) }
    var visible by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        visible++
        onPauseOrDispose { }
    }
    LaunchedEffect(enabled, visible, AvisosNuevos.cambios) {
        // Se cuenta hasta que la pantalla queda visible (visible > 0), para no consultar dos veces
        // al abrirla. Sin mensaje de error: si falla, la campana se queda con el número anterior.
        if (!enabled) sinLeer = 0
        else if (visible > 0) safeCall { NotificationRepository.unreadCount() }.onSuccess { sinLeer = it }
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
