package com.pame.karsy.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.feature.profile.ProfileAvatar

private val MenuGray = Color(0xFF8E9A8E)
private val LogoutCoral = Color(0xFFE65B5B)
private val SoftGray = Color(0xFFF4F7F9)

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
        containerColor = KarsyWhite,
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
                    color = KarsyNavy,
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
        MenuOption(Icons.Outlined.Person, stringResource(R.string.home_my_profile), KarsyNavy) {
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
    if (visible) BackHandler(onBack = onCancel)

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                // Tocar fuera de la tarjeta cancela.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .animateEnterExit(enter = scaleIn(initialScale = 0.9f), exit = scaleOut(targetScale = 0.9f))
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 360.dp)
                    .background(KarsyWhite, RoundedCornerShape(16.dp))
                    // Evita que un toque dentro de la tarjeta la cierre.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(SoftGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = LogoutCoral, modifier = Modifier.size(28.dp))
                }
                Text(
                    stringResource(R.string.home_logout_title),
                    fontFamily = Outfit,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    stringResource(R.string.home_logout_message),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MenuGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 22.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGray, contentColor = KarsyNavy),
                        elevation = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(stringResource(R.string.detail_cancel), fontFamily = DmSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LogoutCoral, contentColor = KarsyWhite),
                        elevation = null,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(stringResource(R.string.home_logout_confirm), fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
