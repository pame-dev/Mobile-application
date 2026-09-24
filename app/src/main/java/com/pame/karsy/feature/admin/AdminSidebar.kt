package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.KarsyLogo
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Secciones del panel de administración (barra lateral del mockup). */
enum class AdminSection(val label: String) {
    Inicio("Inicio"),
    Usuarios("Usuarios"),
    Lotes("Lotes"),
    Vehiculos("Vehículos"),
    Reportes("Reportes"),
    Destacados("Destacados");

    val icon: ImageVector
        get() = when (this) {
            Inicio -> Icons.Outlined.Home
            Usuarios -> Icons.Outlined.Group
            Lotes -> Icons.Outlined.Storefront
            Vehiculos -> Icons.Outlined.DirectionsCar
            Reportes -> Icons.Outlined.Flag
            Destacados -> Icons.Outlined.StarOutline
        }
}

private val AvatarBlue = Color(0xFF3B82F6)

/**
 * Contenido del menú lateral (ModalNavigationDrawer) en azul marino.
 * [badges] = pendientes por sección (reportes, publicaciones, destacados) de la BD.
 */
@Composable
fun AdminDrawerContent(
    drawerState: DrawerState,
    current: AdminSection,
    onSelect: (AdminSection) -> Unit,
    adminName: String,
    badges: Map<AdminSection, Int>,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet(
        drawerState = drawerState,
        drawerContainerColor = KarsyNavy,
        drawerContentColor = KarsyWhite,
        drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
        modifier = Modifier.width(264.dp)
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, bottom = 24.dp)
            ) {
                KarsyLogo(size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Karsy Admin",
                    fontFamily = Outfit,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.03.em,
                    color = KarsyWhite
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                AdminSection.entries.forEach { section ->
                    DrawerItem(
                        section = section,
                        badge = badges[section]?.takeIf { it > 0 },
                        active = section == current,
                        onClick = { onSelect(section) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AvatarBlue)
                ) {
                    Text(
                        adminName.take(1).uppercase().ifEmpty { "A" },
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyWhite
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        adminName.ifEmpty { "Administrador" },
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KarsyWhite.copy(alpha = 0.85f)
                    )
                    Text(
                        "Cerrar sesión",
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                        color = KarsyWhite.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(onClick = onLogout)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(section: AdminSection, badge: Int?, active: Boolean, onClick: () -> Unit) {
    val contentColor = if (active) KarsyWhite else KarsyWhite.copy(alpha = 0.75f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) AdminColors.Blue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) {
        Box {
            Icon(section.icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            if (badge != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AdminColors.BadgeRed)
                        .border(2.dp, if (active) AdminColors.Blue else KarsyNavy, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            section.label,
            fontFamily = DmSans,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        badge?.let { count ->
            Text(
                count.toString(),
                fontFamily = DmSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AdminColors.BadgeRed)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}
