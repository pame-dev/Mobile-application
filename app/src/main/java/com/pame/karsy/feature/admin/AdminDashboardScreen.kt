package com.pame.karsy.feature.admin

import com.pame.karsy.core.navigation.HideBottomBarWhile
import com.pame.karsy.core.navigation.LocalBottomBarSpace
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.R
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.components.KarsyLogo
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.feature.profile.UnreadBadge
import com.pame.karsy.feature.profile.rememberUnreadNotifications
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.Outfit
import kotlinx.coroutines.launch

/** Sección que se pide abrir desde fuera del panel (p. ej. al tocar un aviso de verificación). */
object AdminPanelRequest {
    var section by mutableStateOf<AdminSection?>(null)
}

/**
 * Panel de administración (WebAdminDashboardView del mockup) adaptado a teléfono:
 * la barra lateral se convierte en un ModalNavigationDrawer y las tablas en listas de tarjetas.
 */
@Composable
fun AdminDashboardScreen(
    onCarClick: (Long) -> Unit,
    onReviewReport: (Long) -> Unit,
    onLogout: () -> Unit,
    onNotifications: () -> Unit,
    vm: AdminViewModel = viewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val secciones = AdminSection.delRol(SessionManager.account?.rolPanel)
    // El administrador empieza en Inicio; un moderador, en su primera sección.
    val primera = secciones.first()
    var section by rememberSaveable { mutableStateOf(primera) }
    // Filtro con el que se abre Vehículos o Usuarios desde "Requiere atención"; desde el menú, "Todos".
    var filtro by rememberSaveable { mutableStateOf("Todos") }
    // El menú lateral cubre la pantalla: sin barra inferior mientras se abre o está abierto.
    HideBottomBarWhile(drawerState.isOpen || drawerState.targetValue == DrawerValue.Open)

    // Recarga al volver a la app / a esta pantalla y al cambiar de sección, para que
    // lo que manden los usuarios (solicitudes de destacado, reportes, publicaciones)
    // aparezca sin tener que cerrar sesión.
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(section) { vm.refresh() }
    val pedida = AdminPanelRequest.section
    LaunchedEffect(pedida) {
        if (pedida != null) {
            filtro = "Todos"
            if (pedida in secciones) section = pedida
            AdminPanelRequest.section = null
        }
    }

    // Atrás desde una sección vuelve a la primera (el menú abierto lo maneja ModalDrawerSheet).
    BackHandler(enabled = section != primera && drawerState.isClosed) {
        section = primera
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val stats = vm.stats
            AdminDrawerContent(
                drawerState = drawerState,
                current = section,
                onSelect = {
                    filtro = "Todos"
                    section = it
                    scope.launch { drawerState.close() }
                },
                adminName = SessionManager.account?.nombre.orEmpty(),
                // Los moderadores no leen las estadísticas: cuentan sobre sus listas.
                badges = secciones.associateWith { s ->
                    when (s) {
                        AdminSection.Vehiculos -> stats?.propuestasPendientes ?: vm.vehicles.count { it.pendingProposalId != null }
                        AdminSection.Reportes -> stats?.reportesPendientes ?: vm.reports.count { it.status == "Pendiente" }
                        AdminSection.Destacados -> stats?.destacadosPendientes ?: vm.featuredRequests.count { it.status == FeaturedStatus.Pendiente }
                        AdminSection.Verificaciones -> vm.pendingVerifications
                        else -> 0
                    }
                },
                onLogout = onLogout
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(KarsyBg)
        ) {
            AdminTopBar(
                onMenu = { scope.launch { drawerState.open() } },
                // Misma campana que Inicio y Lotes: se actualiza sola y al llegar un push.
                unread = rememberUnreadNotifications(enabled = true),
                onNotifications = onNotifications
            )
            // Aviso del resultado de la última acción: se oculta solo a los MESSAGE_MS.
            // Se recuerda el último texto para que la animación de salida lo siga mostrando.
            val message = vm.message
            var lastMessage by remember { mutableStateOf("") }
            if (message != null) lastMessage = message
            LaunchedEffect(message) {
                if (message != null) {
                    delay(MESSAGE_MS)
                    vm.dismissMessage()
                }
            }
            AnimatedVisibility(
                visible = message != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KarsyTealLight)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(lastMessage, fontFamily = DmSans, fontSize = 13.sp, color = KarsyInk, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.admin1_close),
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KarsyTeal,
                        modifier = Modifier.clickable { vm.dismissMessage() }
                    )
                }
            }
            if (vm.loading || vm.working) {
                LinearProgressIndicator(color = KarsyTeal, modifier = Modifier.fillMaxWidth())
            }
            // Deslizar hacia abajo recarga todos los datos del panel.
            // Cada sección tiene su propia lista; el espacio de la barra inferior se deja abajo.
            KarsyPullToRefresh(
                onRefresh = { done -> vm.loadAll(done) },
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = LocalBottomBarSpace.current)
            ) {
                // Una sección restaurada que el rol ya no ve cae en la primera permitida.
                when (if (section in secciones) section else primera) {
                    AdminSection.Inicio -> AdminHomeSection(vm = vm, onNavigate = { s, f -> filtro = f ?: "Todos"; section = s })
                    AdminSection.Usuarios -> AdminUsersSection(vm, filtro)
                    AdminSection.Vehiculos -> AdminVehiclesSection(vm, onCarClick, filtro)
                    AdminSection.Reportes -> AdminReportsSection(vm, onCarClick, onReviewReport)
                    AdminSection.Destacados -> AdminFeaturedSection(vm)
                    AdminSection.Verificaciones -> AdminVerificationsSection(vm)
                }
            }
        }
    }
}

@Composable
/** Barra superior del panel. Para volver al inicio se usa la barra inferior. */
private fun AdminTopBar(onMenu: () -> Unit, unread: Int, onNotifications: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(KarsySurface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            TopBarButton(Icons.Rounded.Menu, stringResource(R.string.admin1_open_menu), onMenu)
            Spacer(Modifier.width(12.dp))
            KarsyLogo(size = 32.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                "Karsy Admin",
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 0.03.em,
                color = KarsyInk,
                modifier = Modifier.weight(1f)
            )
            // Campana con el número de avisos sin leer (p. ej. solicitudes de verificación).
            Box {
                TopBarButton(
                    Icons.Rounded.Notifications,
                    if (unread > 0) pluralStringResource(R.plurals.admin2_notifications_unread, unread, unread)
                    else stringResource(R.string.home_notifications),
                    onNotifications
                )
                UnreadBadge(unread)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = KarsyBorder)
    }
}

@Composable
private fun TopBarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(KarsyBg)
            .border(1.5.dp, KarsyBorder, RoundedCornerShape(10.dp))
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = description, tint = KarsyInk, modifier = Modifier.size(19.dp))
        }
    }
}

/** Tiempo que el aviso de una acción del admin se queda visible antes de ocultarse solo. */
private const val MESSAGE_MS = 10_000L
