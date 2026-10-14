package com.pame.karsy

import android.content.Context
import androidx.core.net.toUri
import androidx.compose.runtime.mutableStateOf
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.pame.karsy.core.locale.Idioma
import com.pame.karsy.core.navigation.KarsyNavGraph
import com.pame.karsy.core.push.KarsyMessagingService
import com.pame.karsy.core.session.PublicacionesOcultas
import com.pame.karsy.core.theme.KarsyTheme
import com.pame.karsy.core.theme.Tema

class MainActivity : ComponentActivity() {
    // Cada toque en una notificación push incrementa el contador y abre la bandeja.
    private var openNotificationsRequest by mutableIntStateOf(0)
    // Enlace compartido https://karsy.netlify.app/vehiculo/<id>: (número de petición, id del auto).
    private var openCarRequest by mutableStateOf<Pair<Int, Long>?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Idioma.envolver(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema claro / oscuro elegido en Configuración (o el del teléfono); antes de pintar nada.
        Tema.aplicar(this)
        PublicacionesOcultas.iniciar(this)
        val barras = if (Tema.oscuro) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barras, navigationBarStyle = barras)
        KarsyMessagingService.createChannel(this)
        handlePushIntent(intent)
        // Solo al abrir; al girar la pantalla el enlace ya se atendió.
        if (savedInstanceState == null) handleCarLink(intent)
        setContent {
            KarsyTheme {
                KarsyNavGraph(openNotificationsRequest = openNotificationsRequest, openCarRequest = openCarRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePushIntent(intent)
        handleCarLink(intent)
    }

    private fun handleCarLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW || uri.host != BuildConfig.WEB_URL.toUri().host) return
        val id = uri.pathSegments.takeIf { it.firstOrNull() == "vehiculo" }?.getOrNull(1)?.toLongOrNull() ?: return
        openCarRequest = ((openCarRequest?.first ?: 0) + 1) to id
    }

    private fun handlePushIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(KarsyMessagingService.EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            intent.removeExtra(KarsyMessagingService.EXTRA_OPEN_NOTIFICATIONS)
            openNotificationsRequest++
        }
    }
}
