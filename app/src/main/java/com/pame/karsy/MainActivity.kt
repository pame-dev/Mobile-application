package com.pame.karsy

import android.content.Context
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
import com.pame.karsy.core.theme.KarsyTheme
import com.pame.karsy.core.theme.Tema

class MainActivity : ComponentActivity() {
    // Cada toque en una notificación push incrementa el contador y abre la bandeja.
    private var openNotificationsRequest by mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Idioma.envolver(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema claro / oscuro elegido en Configuración (o el del teléfono); antes de pintar nada.
        Tema.aplicar(this)
        val barras = if (Tema.oscuro) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = barras, navigationBarStyle = barras)
        KarsyMessagingService.createChannel(this)
        handlePushIntent(intent)
        setContent {
            KarsyTheme {
                KarsyNavGraph(openNotificationsRequest = openNotificationsRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(KarsyMessagingService.EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            intent.removeExtra(KarsyMessagingService.EXTRA_OPEN_NOTIFICATIONS)
            openNotificationsRequest++
        }
    }
}
