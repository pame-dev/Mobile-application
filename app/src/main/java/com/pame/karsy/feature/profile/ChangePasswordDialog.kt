package com.pame.karsy.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pame.karsy.R
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PasswordRequirements
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/**
 * Cambiar la contraseña desde el perfil: pide la actual (se comprueba antes de
 * cambiarla) y la nueva dos veces. Ver ProfileViewModel.changePassword.
 */
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (current: String, new: String, confirm: String) -> Unit,
    saving: Boolean = false,
    error: String? = null,
) {
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .imePadding()
                .fillMaxWidth()
                .shadow(24.dp, shape, ambientColor = CardShadow, spotColor = CardShadow)
                .clip(shape)
                .background(KarsySurface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                stringResource(R.string.profile_change_password),
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = KarsyInk
            )
            Text(
                stringResource(R.string.profile_password_desc),
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = KarsyMid,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
            )
            FormInput(
                label = stringResource(R.string.profile_password_current),
                placeholder = "••••••",
                value = current,
                onValueChange = { current = it },
                isPassword = true,
                enabled = !saving
            )
            FormInput(
                label = stringResource(R.string.profile_password_new),
                placeholder = stringResource(R.string.profile_password_new_hint),
                value = new,
                onValueChange = { new = it },
                isPassword = true,
                enabled = !saving
            )
            PasswordRequirements(new, Modifier.padding(start = 4.dp, bottom = 14.dp))
            FormInput(
                label = stringResource(R.string.profile_password_confirm),
                placeholder = "••••••",
                value = confirm,
                onValueChange = { confirm = it },
                isPassword = true,
                enabled = !saving
            )
            error?.let {
                Text(it, fontFamily = DmSans, fontSize = 13.sp, color = KarsyError, modifier = Modifier.padding(bottom = 12.dp))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !saving,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, KarsyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = KarsySurface, contentColor = KarsyTextSecondary),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.profile_cancel), fontFamily = DmSans, fontSize = 14.sp)
                }
                Button(
                    onClick = { onConfirm(current, new, confirm) },
                    enabled = !saving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(2f)
                ) {
                    Text(
                        stringResource(if (saving) R.string.profile_edit_saving else R.string.profile_password_save),
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
