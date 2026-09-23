package com.pame.karsy.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pame.karsy.core.components.karsyTextFieldColors
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.User

/**
 * Modal "Editar perfil" del mockup. Los cambios solo viven en memoria.
 * Para cuentas de lote el nombre es un solo campo ("Nombre del lote").
 */
@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
) {
    val isLote = user.accountType == "lote"
    var firstName by rememberSaveable {
        mutableStateOf(if (isLote) user.displayName else user.displayName.substringBefore(" "))
    }
    var lastName by rememberSaveable {
        mutableStateOf(if (isLote) "" else user.displayName.substringAfter(" ", ""))
    }
    var phone by rememberSaveable { mutableStateOf(user.phone) }
    var email by rememberSaveable { mutableStateOf(user.email) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }

    Dialog(
        onDismissRequest = onDismiss,
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
                .background(KarsyWhite)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Editar perfil",
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = KarsyNavy,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = KarsyMid,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // Avatar con botón de cámara
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    ProfileAvatar(
                        name = user.displayName,
                        avatarUrl = user.avatarUrl,
                        size = 80.dp,
                        initialsSize = 26.sp,
                        modifier = Modifier.border(3.dp, KarsyTeal, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(KarsyWhite)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(KarsyTeal)
                            .clickable {
                                // TODO: abrir selector de imagen y subir a Storage (cuentas.foto_perfil)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = "Cambiar foto",
                            tint = KarsyWhite,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            if (isLote) {
                EditField("Nombre del lote", firstName, { firstName = it })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditField("Nombre", firstName, { firstName = it }, Modifier.weight(1f))
                    EditField("Apellido", lastName, { lastName = it }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            EditField("Teléfono", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(14.dp))
            EditField("Correo electrónico", email, { email = it }, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(14.dp))
            EditField("Descripción", bio, { bio = it }, singleLine = false)

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, KarsyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = KarsyWhite,
                        contentColor = KarsyTextSecondary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", fontFamily = DmSans, fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        val name = if (isLote) firstName.trim()
                        else listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                        // TODO: validar y guardar en public.cuentas (nombre_mostrar, descripcion_corta, foto_perfil)
                        onSave(
                            user.copy(
                                displayName = name.ifEmpty { user.displayName },
                                phone = phone.trim(),
                                email = email.trim().ifEmpty { user.email },
                                bio = bio.trim()
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KarsyNavy,
                        contentColor = KarsyWhite
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.weight(2f)
                ) {
                    Text(
                        "Guardar cambios",
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier) {
        Text(
            label,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            maxLines = if (singleLine) 1 else 5,
            shape = RoundedCornerShape(10.dp),
            colors = karsyTextFieldColors(container = KarsyBg),
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = KarsyCharcoal),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        )
    }
}
