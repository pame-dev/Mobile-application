package com.pame.karsy.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.R
import com.pame.karsy.core.components.ConfirmDialog
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.push.PushTokens
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsySuccess
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Privacidad y seguridad, abierta desde Configuración: cambiar contraseña, cerrar sesión
 * en todos los dispositivos, aviso de privacidad y estado de las notificaciones del teléfono.
 * [onSignedOutEverywhere] lleva a la bienvenida después de cerrar todas las sesiones.
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onPrivacyNotice: () -> Unit,
    onSignedOutEverywhere: () -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passwordOpen by rememberSaveable { mutableStateOf(false) }
    var confirmLogoutAll by rememberSaveable { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Se vuelve a revisar al regresar de los ajustes del teléfono.
    var notificationsOn by remember { mutableStateOf(notificationsEnabled(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { notificationsOn = notificationsEnabled(context) }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KarsyBg)
        ) {
            SubHeader(
                title = stringResource(R.string.privacy_title),
                onBack = onBack,
                modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
            ) {
                message?.let {
                    Text(
                        it,
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        color = KarsyCharcoal,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }

                SectionLabel(R.string.privacy_section_security)
                ProfileCard {
                    PrivacyRow("🔑", R.string.privacy_change_password, stringResource(R.string.privacy_change_password_desc)) {
                        vm.clearDialogErrors()
                        passwordOpen = true
                    }
                    RowDivider()
                    PrivacyRow("📱", R.string.privacy_logout_all, stringResource(R.string.privacy_logout_all_desc)) {
                        confirmLogoutAll = true
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionLabel(R.string.privacy_section_privacy)
                ProfileCard {
                    PrivacyRow("📄", R.string.privacy_notice, stringResource(R.string.privacy_notice_desc), onClick = onPrivacyNotice)
                    RowDivider()
                    PrivacyRow(
                        icon = "🔔",
                        labelRes = R.string.privacy_device_notifications,
                        description = stringResource(
                            if (notificationsOn) R.string.privacy_device_notifications_on
                            else R.string.privacy_device_notifications_off
                        ),
                        descriptionColor = if (notificationsOn) KarsySuccess else KarsyError,
                    ) {
                        // Ajustes de notificaciones de Karsy en el teléfono.
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                }
            }
        }

        ConfirmDialog(
            visible = confirmLogoutAll,
            icon = Icons.AutoMirrored.Rounded.Logout,
            accent = KarsyError,
            title = stringResource(R.string.privacy_logout_all_confirm_title),
            message = stringResource(R.string.privacy_logout_all_confirm_body),
            confirmText = stringResource(R.string.privacy_logout_all_confirm),
            onCancel = { confirmLogoutAll = false },
            onConfirm = {
                if (loggingOut) return@ConfirmDialog
                loggingOut = true
                scope.launch {
                    // Antes de cerrar (después ya no hay permiso): este teléfono deja de recibir avisos.
                    PushTokens.unregister(context)
                    safeCall { AuthRepository.signOutEverywhere() }
                        .onSuccess { onSignedOutEverywhere() }
                        .onFailure { message = it.mensajeUsuario() }
                    loggingOut = false
                    confirmLogoutAll = false
                }
            }
        )
    }

    if (passwordOpen) {
        ChangePasswordDialog(
            saving = vm.saving,
            error = vm.passwordError,
            onDismiss = { passwordOpen = false },
            onConfirm = { current, new, confirm ->
                vm.changePassword(current, new, confirm) {
                    passwordOpen = false
                    message = vm.message
                }
            }
        )
    }
}

/** Aviso de privacidad (mismo layout que Términos y condiciones). */
@Composable
fun PrivacyNoticeScreen(onBack: () -> Unit) = LegalDocumentScreen(
    titleRes = R.string.privacy_notice_title,
    lastUpdateRes = R.string.privacy_notice_last_update,
    introRes = R.string.privacy_notice_intro,
    sections = listOf(
        LegalSection(R.string.privacy_notice_s1_title, R.string.privacy_notice_s1_body),
        LegalSection(R.string.privacy_notice_s2_title, R.string.privacy_notice_s2_body),
        LegalSection(R.string.privacy_notice_s3_title, R.string.privacy_notice_s3_body),
        LegalSection(R.string.privacy_notice_s4_title, R.string.privacy_notice_s4_body),
        LegalSection(R.string.privacy_notice_s5_title, R.string.privacy_notice_s5_body),
        LegalSection(R.string.privacy_notice_s6_title, R.string.privacy_notice_s6_body),
    ),
    onBack = onBack,
)

/** true si Karsy puede mostrar notificaciones en este teléfono. */
private fun notificationsEnabled(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) return false
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@Composable
private fun SectionLabel(@StringRes textRes: Int) {
    Text(
        stringResource(textRes).uppercase(),
        fontFamily = DmSans,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = KarsyMid,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun PrivacyRow(
    icon: String,
    @StringRes labelRes: Int,
    description: String,
    descriptionColor: Color = KarsyMid,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(icon, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(labelRes),
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = KarsyCharcoal
            )
            Spacer(Modifier.height(2.dp))
            Text(description, fontFamily = DmSans, fontSize = 13.sp, color = descriptionColor)
        }
        RowChevron()
    }
}
