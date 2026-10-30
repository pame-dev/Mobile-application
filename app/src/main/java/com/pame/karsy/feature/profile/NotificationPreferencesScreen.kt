package com.pame.karsy.feature.profile

import android.widget.Toast
import com.pame.karsy.core.components.KarsyPullToRefresh
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.SkeletonListRow
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.PreferenciasAvisosDto
import com.pame.karsy.data.repository.NotificationRepository
import kotlinx.coroutines.launch

/**
 * Qué avisos opcionales recibe la cuenta (preferencias_notificacion). Los de la cuenta
 * (aprobaciones, rechazos, reportes) y las búsquedas guardadas siempre llegan.
 */
@Composable
fun NotificationPreferencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prefs by remember { mutableStateOf<PreferenciasAvisosDto?>(null) }
    suspend fun load() {
        prefs = safeCall { NotificationRepository.preferences() }.getOrDefault(prefs ?: PreferenciasAvisosDto())
    }
    LaunchedEffect(Unit) { load() }

    /** Se guarda al momento; si falla, se regresa el interruptor y se avisa. */
    fun cambiar(nuevo: PreferenciasAvisosDto) {
        val antes = prefs
        prefs = nuevo
        scope.launch {
            safeCall { NotificationRepository.savePreferences(nuevo) }.onFailure {
                prefs = antes
                Toast.makeText(context, it.mensajeUsuario(), Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = stringResource(R.string.notif_prefs_title),
            onBack = onBack,
            modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
        )
        // Jalar hacia abajo vuelve a leer las preferencias (por si se cambiaron en la web).
        KarsyPullToRefresh(onRefresh = { done -> scope.launch { load(); done() } }, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
        ) {
            val p = prefs
            ProfileCard {
                if (p == null) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { repeat(4) { SkeletonListRow() } }
                    return@ProfileCard
                }
                Column {
                    PrefRow(R.string.notif_prefs_prices, R.string.notif_prefs_prices_desc, p.precios) { cambiar(p.copy(precios = it)) }
                    RowDivider()
                    PrefRow(R.string.notif_prefs_summaries, R.string.notif_prefs_summaries_desc, p.resumenes) { cambiar(p.copy(resumenes = it)) }
                    RowDivider()
                    PrefRow(R.string.notif_prefs_recommendations, R.string.notif_prefs_recommendations_desc, p.recomendaciones) { cambiar(p.copy(recomendaciones = it)) }
                    RowDivider()
                    PrefRow(R.string.notif_prefs_seller, R.string.notif_prefs_seller_desc, p.vendedor) { cambiar(p.copy(vendedor = it)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.notif_prefs_footer),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                lineHeight = 17.sp,
                color = KarsyMid,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        }
    }
}

@Composable
private fun PrefRow(@StringRes title: Int, @StringRes desc: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), fontFamily = DmSans, fontSize = KarsyTextSize.BodyLarge, fontWeight = FontWeight.SemiBold, color = KarsyCharcoal)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(desc), fontFamily = DmSans, fontSize = KarsyTextSize.Small, lineHeight = 17.sp, color = KarsyMid)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = KarsyWhite, checkedTrackColor = KarsyTeal)
        )
    }
}
