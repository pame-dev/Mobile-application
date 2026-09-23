package com.pame.karsy.feature.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.KarsyLogo
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import kotlinx.coroutines.launch

/**
 * Panel de administración (WebAdminDashboardView del mockup) adaptado a teléfono:
 * la barra lateral se convierte en un ModalNavigationDrawer y las tablas en listas de tarjetas.
 */
@Composable
fun AdminDashboardScreen(onBack: () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var section by rememberSaveable { mutableStateOf(AdminSection.Inicio) }

    // Atrás desde una sección vuelve a Inicio (el menú abierto lo maneja ModalDrawerSheet).
    BackHandler(enabled = section != AdminSection.Inicio && drawerState.isClosed) {
        section = AdminSection.Inicio
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AdminDrawerContent(
                drawerState = drawerState,
                current = section,
                onSelect = {
                    section = it
                    scope.launch { drawerState.close() }
                }
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
            Box(Modifier.weight(1f)) {
                when (section) {
                    AdminSection.Inicio -> AdminHomeSection(onNavigate = { section = it })
                    AdminSection.Usuarios -> AdminUsersSection()
                    AdminSection.Lotes -> AdminLotesSection()
                    AdminSection.Vehiculos -> AdminVehiclesSection()
                    AdminSection.Reportes -> AdminReportsSection()
                    AdminSection.Destacados -> AdminFeaturedSection()
                }
            }
        }
    }
}

@Composable
private fun AdminTopBar(onMenu: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(KarsyWhite)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            TopBarButton(Icons.Rounded.Menu, "Abrir menú", onMenu)
            Spacer(Modifier.width(12.dp))
            KarsyLogo(size = 32.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                "Karsy Admin",
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 0.03.em,
                color = KarsyNavy,
                modifier = Modifier.weight(1f)
            )
            TopBarButton(Icons.Outlined.Home, "Volver al inicio", onHome)
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
            Icon(icon, contentDescription = description, tint = KarsyNavy, modifier = Modifier.size(19.dp))
        }
    }
}
