package com.pame.karsy.feature.profile

import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.core.app.NotificationManagerCompat
import com.pame.karsy.core.components.AvisoKarsy
import com.pame.karsy.core.locale.rememberTraduccion
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
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VerifiedUser
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
import com.pame.karsy.core.theme.KarsyAmber
import com.pame.karsy.core.theme.KarsyGreen
import com.pame.karsy.core.theme.KarsyGreenSoft
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsyRed
import com.pame.karsy.core.theme.KarsyRedSoft
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyTealText
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.util.abrirAjustesDeNotificaciones
import com.pame.karsy.core.supabase.mensajeUsuario
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

/** Ícono y colores de cada tipo de aviso. */
private data class NotifStyle(val icon: ImageVector, val color: Color, val bg: Color)

/**
 * Solo tres colores (el ícono dice de qué trata): verde = buena noticia, rojo = problema o
 * rechazo, turquesa = informativo (todo lo demás). Mismos que la web (Account.tsx).
 */
private fun styleFor(tipo: String): NotifStyle {
    fun bueno(icon: ImageVector) = NotifStyle(icon, KarsyGreen, KarsyGreenSoft)
    fun malo(icon: ImageVector) = NotifStyle(icon, KarsyRed, KarsyRedSoft)
    // Turquesa de texto: se lee mejor sobre el fondo claro (en oscuro es el mismo turquesa).
    fun info(icon: ImageVector) = NotifStyle(icon, KarsyTealText, KarsyTealSoft)
    return when (tipo) {
        "favorito" -> info(Icons.Rounded.Favorite)
        "propuesta_enviada", "publicacion_por_revisar", "edicion_por_revisar" -> info(Icons.Rounded.HourglassTop)
        "reporte_nuevo" -> info(Icons.Rounded.Flag)
        "publicacion_aprobada", "edicion_aprobada", "publicacion_rehabilitada" -> bueno(Icons.Rounded.CheckCircle)
        "destacado_aprobado" -> bueno(Icons.Rounded.Star)
        "destacado_por_vencer" -> info(Icons.Rounded.Star)
        "pago_destacado" -> info(Icons.Rounded.Payments)
        "publicacion_rechazada", "edicion_rechazada" -> malo(Icons.Rounded.Cancel)
        "publicacion_deshabilitada", "cuenta_suspendida" -> malo(Icons.Rounded.Block)
        "cuenta_reactivada" -> bueno(Icons.Rounded.CheckCircle)
        "verificacion_pendiente" -> info(Icons.Rounded.VerifiedUser)
        "verificacion_aprobada" -> bueno(Icons.Rounded.VerifiedUser)
        "verificacion_rechazada" -> malo(Icons.Rounded.Cancel)
        "destacado_rechazado" -> malo(Icons.Rounded.StarOutline)
        "reporte_atendido", "reporte_descartado" -> info(Icons.Rounded.Flag)
        "busqueda_coincidencia" -> info(Icons.Rounded.NotificationsActive)
        "precio_bajo" -> info(Icons.Rounded.TrendingDown)
        "favorito_no_disponible" -> info(Icons.Rounded.HeartBroken)
        "resumen_semanal", "auto_parecido" -> info(Icons.Rounded.DirectionsCar)
        "resumen_vendedor", "hito_vistas" -> info(Icons.Rounded.Insights)
        "poca_actividad", "confirmar_venta" -> info(Icons.Rounded.Sell)
        else -> info(Icons.Rounded.Notifications)
    }
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
    /** Solicitud de verificación nueva (admin) → Verificación en el panel. */
    onAdminVerifications: () -> Unit = {},
    /** Pago de destacado por revisar (admin) → Destacados en el panel. */
    onAdminFeatured: () -> Unit = {},
    /** Publicación o edición por revisar (admin y moderador de publicaciones) → Vehículos en el panel. */
    onAdminVehicles: () -> Unit = {},
    /** Reporte nuevo (admin y moderador de publicaciones) → Reportes en el panel. */
    onAdminReports: () -> Unit = {},
    /** Verificación aprobada o rechazada → su perfil (ahí ve la insignia o la reenvía). */
    onProfile: () -> Unit = {},
) {
    var notifications by remember { mutableStateOf<List<NotificacionDto>?>(null) }
    /** Falló la primera carga: se dice en lugar de mostrar "sin avisos". */
    var errorCarga by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun load() {
        safeCall { NotificationRepository.mine() }
            .onSuccess { lista ->
                if (lista.any { !it.leida }) safeCall { NotificationRepository.markAllRead() }
                notifications = lista
                errorCarga = null
            }
            // Al refrescar sin red se conserva lo que ya se veía.
            .onFailure { if (notifications == null) errorCarga = it.mensajeUsuario() else AvisoKarsy.mostrar(it.mensajeUsuario(), error = true) }
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
                errorCarga?.takeIf { items == null }?.let { msg ->
                    ProfileLoadError(msg) { errorCarga = null; scope.launch { load() } }
                    return@Column
                }
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
                            fontSize = KarsyTextSize.BodyLarge,
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
                                    n.tipo == "verificacion_pendiente" -> onAdminVerifications
                                    n.tipo == "pago_destacado" -> onAdminFeatured
                                    n.tipo == "publicacion_por_revisar" || n.tipo == "edicion_por_revisar" -> onAdminVehicles
                                    n.tipo == "reporte_nuevo" -> onAdminReports
                                    n.tipo == "verificacion_aprobada" || n.tipo == "verificacion_rechazada" -> onProfile
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
            .clip(RoundedCornerShape(KarsyRadius.Md))
            .background(karsyTint(style.color, style.bg))
            .then(if (n.leida) Modifier else Modifier.border(1.dp, style.color.copy(alpha = 0.35f), RoundedCornerShape(KarsyRadius.Md)))
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(KarsyRadius.Sm))
                .background(KarsySurface)
        ) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notificationText(LocalContext.current, n.tipo, vehiculo, datosDeAviso(n.datos)),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                fontWeight = if (n.leida) FontWeight.Medium else FontWeight.SemiBold,
                lineHeight = 17.sp,
                color = KarsyCharcoal
            )
            // Comentario del admin (por qué se rechazó o deshabilitó).
            n.motivo?.takeIf { it.isNotBlank() }?.let { motivo ->
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.notif_reason, rememberTraduccion(motivo)),
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Small,
                    lineHeight = 17.sp,
                    color = style.color
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                Formato.haceCuanto(n.fecha),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Caption,
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
                            abrirAjustesDeNotificaciones(context)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsOff, contentDescription = null, tint = KarsyAmber, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.notif_prefs_system_off),
                        fontFamily = DmSans,
                        fontSize = KarsyTextSize.Body,
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
                    fontSize = KarsyTextSize.BodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyCharcoal,
                    modifier = Modifier.weight(1f)
                )
                RowChevron()
            }
        }
    }
}
