package com.pame.karsy.feature.admin

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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Menu
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
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import kotlinx.coroutines.launch

/**
 * Panel de administración (WebAdminDashboardView del mockup) adaptado a teléfono:
 * la barra lateral se convierte en un ModalNavigationDrawer y las tablas en listas de tarjetas.
 */
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    onCarClick: (Long) -> Unit,
    onReviewReport: (Long) -> Unit,
    onLogout: () -> Unit,
    vm: AdminViewModel = viewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(AdminSection.Inicio) }

    // Recarga al volver a la app / a esta pantalla y al cambiar de sección, para que
    // lo que manden los usuarios (solicitudes de destacado, reportes, publicaciones)
    // aparezca sin tener que cerrar sesión.
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(section) { vm.refresh() }

    // Atrás desde una sección vuelve a Inicio (el menú abierto lo maneja ModalDrawerSheet).
    BackHandler(enabled = section != AdminSection.Inicio && drawerState.isClosed) {
        section = AdminSection.Inicio
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val stats = vm.stats
            AdminDrawerContent(
                drawerState = drawerState,
                current = section,
                onSelect = {
                    section = it
                    scope.launch { drawerState.close() }
                },
                adminName = SessionManager.account?.nombre.orEmpty(),
                badges = if (stats == null) emptyMap() else mapOf(
                    AdminSection.Vehiculos to stats.propuestasPendientes,
                    AdminSection.Reportes to stats.reportesPendientes,
                    AdminSection.Destacados to stats.destacadosPendientes,
                ),
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
                onHome = onBack
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
            KarsyPullToRefresh(onRefresh = { done -> vm.loadAll(done) }, modifier = Modifier.weight(1f)) {
                when (section) {
                    AdminSection.Inicio -> AdminHomeSection(vm = vm, onNavigate = { section = it })
                    AdminSection.Usuarios -> AdminUsersSection(vm)
                    AdminSection.Vehiculos -> AdminVehiclesSection(vm, onCarClick)
                    AdminSection.Reportes -> AdminReportsSection(vm, onCarClick, onReviewReport)
                    AdminSection.Destacados -> AdminFeaturedSection(vm)
                }
            }
        }
    }
}

@Composable
private fun AdminTopBar(onMenu: () -> Unit, onHome: () -> Unit) {
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
            TopBarButton(Icons.Outlined.Home, stringResource(R.string.admin1_back_home), onHome)
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
