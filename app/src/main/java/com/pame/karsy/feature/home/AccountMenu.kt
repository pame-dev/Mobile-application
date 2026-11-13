package com.pame.karsy.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.ConfirmDialog
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyCoral
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.feature.profile.ProfileAvatar

private val MenuGray: Color get() = KarsyMid
private val LogoutCoral = KarsyCoral

/**
 * Menú flotante del ícono de perfil (solo con sesión). El encabezado (foto, nombre y correo)
 * lleva a Mi perfil. Debajo, en grupos: TU CUENTA ("Mis autos") y ADMINISTRACIÓN ("Panel de
 * administración" con sus [pendientes], admin o moderador). Al final "Idioma" y "Cerrar sesión".
 * [panelActivo] / [adminActivo] marcan la opción de la pantalla en la que ya se está.
 */
@Composable
internal fun AccountMenu(
    expanded: Boolean,
    name: String,
    email: String,
    avatarUrl: String?,
    onDismiss: () -> Unit,
    onProfile: () -> Unit,
    showPanel: Boolean,
    onPanel: () -> Unit,
    showAdminPanel: Boolean,
    onAdminPanel: () -> Unit,
    onLanguage: () -> Unit,
    onLogout: () -> Unit,
    pendientes: Int = 0,
    panelActivo: Boolean = false,
    adminActivo: Boolean = false,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 8.dp),
        shape = RoundedCornerShape(KarsyRadius.Lg),
        containerColor = KarsySurface,
        shadowElevation = 10.dp,
        modifier = Modifier.widthIn(min = 240.dp, max = 300.dp)
    ) {
        // Tocar la foto o el nombre abre Mi perfil (ya no hay fila aparte).
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    onDismiss()
                    onProfile()
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileAvatar(name = name, avatarUrl = avatarUrl, size = 42.dp, initialsSize = 15.sp)
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontFamily = Outfit,
                    fontSize = KarsyTextSize.Subtitle,
                    fontWeight = FontWeight.Bold,
                    color = KarsyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    email.ifBlank { stringResource(R.string.home_my_profile) },
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Small,
                    color = MenuGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MenuGray, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color = KarsyBorder)
        if (showPanel) {
            GroupLabel(stringResource(R.string.admin2_group_account))
            MenuOption(Icons.Outlined.SpaceDashboard, stringResource(R.string.profile_panel_seller), KarsyInk, active = panelActivo) {
                onDismiss()
                onPanel()
            }
        }
        if (showAdminPanel) {
            GroupLabel(stringResource(R.string.home_menu_group_admin))
            MenuOption(
                Icons.Outlined.AdminPanelSettings,
                stringResource(R.string.home_admin_panel),
                KarsyInk,
                active = adminActivo,
                count = pendientes
            ) {
                onDismiss()
                onAdminPanel()
            }
        }
        if (showPanel || showAdminPanel) HorizontalDivider(color = KarsyBorder, modifier = Modifier.padding(top = 4.dp))
        // A la vista (antes solo estaba en Ajustes): los usuarios no lo encontraban.
        MenuOption(Icons.Outlined.Language, stringResource(R.string.settings_language), KarsyInk) {
            onDismiss()
            onLanguage()
        }
        HorizontalDivider(color = KarsyBorder)
        MenuOption(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.settings_logout), LogoutCoral) {
            onDismiss()
            onLogout()
        }
    }
}

/** Título de grupo del menú (en mayúsculas, gris, como en el menú del panel). */
@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        fontFamily = DmSans,
        fontSize = KarsyTextSize.Small,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MenuGray,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
    )
}

/**
 * Opción del menú. [active]: es la pantalla en la que ya se está (fondo suave y texto en
 * negritas). [count] > 0 muestra una pastilla con los pendientes.
 */
@Composable
private fun MenuOption(
    icon: ImageVector,
    text: String,
    color: Color,
    active: Boolean = false,
    count: Int = 0,
    onClick: () -> Unit,
) {
    val pendingCd = if (count > 0) stringResource(R.string.home_menu_pending_cd, count) else null
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(KarsyRadius.Md))
            .background(if (active) KarsyBorder.copy(alpha = 0.6f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (active) selected = true
                if (pendingCd != null) stateDescription = pendingCd
            }
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        Text(
            text,
            fontFamily = DmSans,
            fontSize = KarsyTextSize.BodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        if (count > 0) {
            Text(
                if (count > 99) "99+" else count.toString(),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                fontWeight = FontWeight.Bold,
                color = KarsyWhite,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(KarsyCoral)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clearAndSetSemantics {}
            )
        }
    }
}

/**
 * Menú del ícono de Perfil para un visitante: iniciar sesión, crear cuenta e idioma. Así el
 * idioma se puede cambiar en cualquier momento (como el selector de la barra de la web).
 */
@Composable
internal fun VisitorMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLanguage: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 8.dp),
        shape = RoundedCornerShape(KarsyRadius.Lg),
        containerColor = KarsySurface,
        shadowElevation = 10.dp,
        modifier = Modifier.widthIn(min = 220.dp, max = 300.dp)
    ) {
        MenuOption(Icons.AutoMirrored.Rounded.Login, stringResource(R.string.home_login), KarsyInk) {
            onDismiss()
            onLogin()
        }
        MenuOption(Icons.Outlined.PersonAdd, stringResource(R.string.home_register), KarsyInk) {
            onDismiss()
            onRegister()
        }
        HorizontalDivider(color = KarsyBorder)
        MenuOption(Icons.Outlined.Language, stringResource(R.string.settings_language), KarsyInk) {
            onDismiss()
            onLanguage()
        }
    }
}

/** Modal "¿Cerrar sesión?" sobre un fondo oscuro que cubre toda la pantalla. */
@Composable
internal fun LogoutConfirmDialog(
    visible: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        visible = visible,
        icon = Icons.AutoMirrored.Rounded.Logout,
        accent = LogoutCoral,
        title = stringResource(R.string.home_logout_title),
        message = stringResource(R.string.home_logout_message),
        confirmText = stringResource(R.string.home_logout_confirm),
        onCancel = onCancel,
        onConfirm = onConfirm
    )
}
