package com.pame.karsy.feature.profile

import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.core.app.NotificationManagerCompat
import android.provider.Settings
import android.content.Intent
import com.pame.karsy.core.push.datosDeAviso
import com.pame.karsy.core.push.notificationText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.components.SkeletonListRow
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.karsyTint
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.NotificacionDto
import com.pame.karsy.data.repository.NotificationRepository

/** Ícono, colores y texto de cada tipo de aviso (mismo estilo que las alertas del admin). */
private data class NotifStyle(val icon: ImageVector, val color: Color, val bg: Color)

private fun styleFor(tipo: String): NotifStyle = when (tipo) {
    "favorito" -> NotifStyle(Icons.Rounded.Favorite, Color(0xFFEF4444), Color(0xFFFEF2F2))
    "propuesta_enviada" -> NotifStyle(Icons.Rounded.HourglassTop, Color(0xFFD97706), Color(0xFFFFFBEB))
    "publicacion_aprobada", "edicion_aprobada", "publicacion_rehabilitada" ->
        NotifStyle(Icons.Rounded.CheckCircle, Color(0xFF16A34A), Color(0xFFF0FDF4))
    "destacado_aprobado" -> NotifStyle(Icons.Rounded.Star, Color(0xFF8B5CF6), Color(0xFFF5F3FF))
    "destacado_por_vencer" -> NotifStyle(Icons.Rounded.Star, Color(0xFFD97706), Color(0xFFFFFBEB))
    "publicacion_rechazada", "edicion_rechazada" -> NotifStyle(Icons.Rounded.Cancel, Color(0xFFDC2626), Color(0xFFFEF2F2))
    "publicacion_deshabilitada", "cuenta_suspendida" -> NotifStyle(Icons.Rounded.Block, Color(0xFFDC2626), Color(0xFFFEF2F2))
    "cuenta_reactivada" -> NotifStyle(Icons.Rounded.CheckCircle, Color(0xFF16A34A), Color(0xFFF0FDF4))
    "destacado_rechazado" -> NotifStyle(Icons.Rounded.StarOutline, Color(0xFFD97706), Color(0xFFFFFBEB))
    "reporte_atendido" -> NotifStyle(Icons.Rounded.Flag, Color(0xFF2563EB), Color(0xFFEFF6FF))
    "reporte_descartado" -> NotifStyle(Icons.Rounded.Flag, KarsyTeal, KarsyBg)
    "busqueda_coincidencia" -> NotifStyle(Icons.Rounded.NotificationsActive, KarsyTeal, Color(0xFFE6F6F6))
    "precio_bajo" -> NotifStyle(Icons.Rounded.TrendingDown, Color(0xFF16A34A), Color(0xFFF0FDF4))
    "favorito_no_disponible" -> NotifStyle(Icons.Rounded.HeartBroken, Color(0xFF64748B), Color(0xFFF1F5F9))
    "resumen_semanal", "auto_parecido" -> NotifStyle(Icons.Rounded.DirectionsCar, KarsyTeal, Color(0xFFE6F6F6))
    "resumen_vendedor", "hito_vistas" -> NotifStyle(Icons.Rounded.Insights, Color(0xFF8B5CF6), Color(0xFFF5F3FF))
    "poca_actividad", "confirmar_venta" -> NotifStyle(Icons.Rounded.Sell, Color(0xFFD97706), Color(0xFFFFFBEB))
    else -> NotifStyle(Icons.Rounded.Notifications, KarsyTeal, KarsyBg)
}

/**
 * Bandeja de notificaciones del usuario, abierta desde Configuración.
 * Al abrirla, los avisos nuevos se marcan como leídos (se ven resaltados esta vez).
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onCarClick: (Long) -> Unit,
    onPreferences: () -> Unit = {},
    /** Resumen semanal de autos nuevos → inicio. */
    onHome: () -> Unit = {},
    /** Resumen semanal del vendedor → su panel. */
    onDashboard: () -> Unit = {},
) {
    var notifications by remember { mutableStateOf<List<NotificacionDto>?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        val lista = safeCall { NotificationRepository.mine() }.getOrDefault(emptyList())
        if (lista.any { !it.leida }) safeCall { NotificationRepository.markAllRead() }
        notifications = lista
    }
    LaunchedEffect(Unit) { load() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = stringResource(R.string.notif_title),
            onBack = onBack,
            modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
        )
        KarsyPullToRefresh(onRefresh = { done -> scope.launch { load(); done() } }, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
            ) {
                PreferencesRow(onPreferences)
                Spacer(Modifier.height(14.dp))
                val items = notifications
                if (items == null) {
                    // Esqueleto de la lista mientras carga.
                    ProfileCard {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            repeat(5) { SkeletonListRow() }
                        }
                    }
                    return@Column
                }
                ProfileCard {
                    if (items.isEmpty()) {
                        Text(
                            stringResource(R.string.notif_empty),
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                            color = KarsyMid,
                            modifier = Modifier.padding(20.dp)
                        )
                        return@ProfileCard
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        items.forEach { n ->
                            NotificationRow(
                                n,
                                onOpen = when {
                                    n.tipo == "resumen_semanal" -> onHome
                                    n.tipo == "resumen_vendedor" -> onDashboard
                                    n.idPublicacion != null -> ({ onCarClick(n.idPublicacion) })
                                    else -> null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(n: NotificacionDto, onOpen: (() -> Unit)?) {
    val style = styleFor(n.tipo)
    val vehiculo = n.tituloVehiculo?.takeIf { it.isNotBlank() } ?: stringResource(R.string.notif_vehicle_fallback)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(karsyTint(style.color, style.bg))
            .then(if (n.leida) Modifier else Modifier.border(1.dp, style.color.copy(alpha = 0.35f), RoundedCornerShape(10.dp)))
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KarsySurface)
        ) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notificationText(LocalContext.current, n.tipo, vehiculo, datosDeAviso(n.datos)),
                fontFamily = DmSans,
                fontSize = 12.sp,
                fontWeight = if (n.leida) FontWeight.Medium else FontWeight.SemiBold,
                lineHeight = 17.sp,
                color = KarsyCharcoal
            )
            // Comentario del admin (por qué se rechazó o deshabilitó).
            n.motivo?.takeIf { it.isNotBlank() }?.let { motivo ->
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.notif_reason, motivo),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = style.color
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                Formato.haceCuanto(n.fecha),
                fontFamily = DmSans,
                fontSize = 11.sp,
                color = KarsyMid
            )
        }
        if (!n.leida) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(style.color)
            )
        }
    }
}

/**
 * "Elegir qué avisos recibir" y, si el teléfono tiene apagadas las notificaciones de
 * Karsy, un aviso con el botón para activarlas en los ajustes de Android.
 */
@Composable
private fun PreferencesRow(onPreferences: () -> Unit) {
    val context = LocalContext.current
    val permitidas = NotificationManagerCompat.from(context).areNotificationsEnabled()
    ProfileCard {
        Column {
            if (!permitidas) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            runCatching { context.startActivity(intent) }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.notif_prefs_system_off),
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = KarsyCharcoal,
                        modifier = Modifier.weight(1f)
                    )
                }
                RowDivider()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPreferences)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.notif_prefs_title),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyCharcoal,
                    modifier = Modifier.weight(1f)
                )
                RowChevron()
            }
        }
    }
}
