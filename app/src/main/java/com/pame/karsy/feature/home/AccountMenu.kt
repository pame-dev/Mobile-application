package com.pame.karsy.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Person
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
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.feature.profile.ProfileAvatar

private val MenuGray = Color(0xFF8E9A8E)
private val LogoutCoral = Color(0xFFE65B5B)

/**
 * Menú flotante del ícono de perfil (solo con sesión): datos de la cuenta,
 * "Mi perfil" y "Cerrar sesión". Se ancla debajo del ícono y se cierra al tocar fuera.
 */
@Composable
internal fun AccountMenu(
    expanded: Boolean,
    name: String,
    email: String,
    avatarUrl: String?,
    onDismiss: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 8.dp),
        shape = RoundedCornerShape(14.dp),
        containerColor = KarsySurface,
        shadowElevation = 10.dp,
        modifier = Modifier.widthIn(min = 240.dp, max = 300.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileAvatar(name = name, avatarUrl = avatarUrl, size = 42.dp, initialsSize = 15.sp)
            Column {
                Text(
                    name,
                    fontFamily = Outfit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (email.isNotBlank()) Text(
                    email,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    color = MenuGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HorizontalDivider(color = KarsyBorder)
        MenuOption(Icons.Outlined.Person, stringResource(R.string.home_my_profile), KarsyInk) {
            onDismiss()
            onProfile()
        }
        HorizontalDivider(color = KarsyBorder)
        MenuOption(Icons.AutoMirrored.Rounded.Logout, stringResource(R.string.settings_logout), LogoutCoral) {
            onDismiss()
            onLogout()
        }
    }
}

@Composable
private fun MenuOption(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        Text(text, fontFamily = DmSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
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
