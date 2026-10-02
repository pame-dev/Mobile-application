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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.components.karsyTextFieldColors
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.User
import com.pame.karsy.data.repository.UserRepository

/**
 * Modal "Editar perfil" del mockup. Guarda en public.cuentas / perfiles_lote /
 * telefonos_contacto (ver ProfileViewModel). Para cuentas de lote el nombre es un solo
 * campo ("Nombre del lote") y se edita también el responsable y la dirección.
 * El correo se muestra pero no se puede cambiar.
 */
@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    onPickPhoto: () -> Unit,
    onChangePassword: () -> Unit,
    saving: Boolean = false,
    error: String? = null,
) {
    val isLote = user.accountType == "lote"
    var firstName by rememberSaveable {
        mutableStateOf(if (isLote) user.displayName else user.displayName.substringBefore(" "))
    }
    var lastName by rememberSaveable {
        mutableStateOf(if (isLote) "" else user.displayName.substringAfter(" ", ""))
    }
    var phone by rememberSaveable { mutableStateOf(user.phone) }
    var whatsapp by rememberSaveable { mutableStateOf(user.whatsapp) }
    var medio by rememberSaveable { mutableStateOf(user.medioPrincipal) }
    var phonesError by rememberSaveable { mutableStateOf<String?>(null) }
    var bio by rememberSaveable { mutableStateOf(user.bio) }
    var municipio by rememberSaveable { mutableStateOf(user.municipio) }
    var estado by rememberSaveable { mutableStateOf(user.estado) }
    var responsable by rememberSaveable { mutableStateOf(user.responsable) }
    var calle by rememberSaveable { mutableStateOf(user.calle) }
    var numero by rememberSaveable { mutableStateOf(user.numero) }
    var colonia by rememberSaveable { mutableStateOf(user.colonia) }
    var codigoPostal by rememberSaveable { mutableStateOf(user.codigoPostal) }
    var missing by rememberSaveable { mutableStateOf(false) }

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
                    stringResource(R.string.profile_edit_profile),
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = KarsyNavy,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.profile_close_cd),
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
                            .clickable(onClick = onPickPhoto),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = stringResource(R.string.profile_change_photo_cd),
                            tint = KarsyWhite,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            if (isLote) {
                EditField(stringResource(R.string.profile_edit_lot_name), firstName, { firstName = it })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditField(stringResource(R.string.profile_edit_first_name), firstName, { firstName = it }, Modifier.weight(1f))
                    EditField(stringResource(R.string.profile_edit_last_name), lastName, { lastName = it }, Modifier.weight(1f))
                }
            }
            if (isLote) {
                Spacer(Modifier.height(14.dp))
                EditField(stringResource(R.string.profile_edit_responsible), responsable, { responsable = it })
            }
            Spacer(Modifier.height(14.dp))
            EditField(stringResource(R.string.profile_edit_phone), phone, { phone = it }, keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(14.dp))
            EditField(stringResource(R.string.profile_edit_whatsapp), whatsapp, { whatsapp = it }, keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            MainContactPicker(selected = medio, onSelect = { medio = it })
            phonesError?.let { Text(it, fontFamily = DmSans, fontSize = 12.sp, color = KarsyError, modifier = Modifier.padding(top = 4.dp)) }
            Spacer(Modifier.height(14.dp))
            // El correo identifica la cuenta: se muestra pero no se edita.
            EditField(stringResource(R.string.profile_edit_email), user.email, {}, enabled = false)
            Text(
                stringResource(R.string.profile_edit_email_locked),
                fontFamily = DmSans,
                fontSize = 11.sp,
                color = KarsyMid,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                EditField(stringResource(R.string.profile_edit_city), municipio, { municipio = it }, Modifier.weight(1f))
                EditField(stringResource(R.string.profile_edit_state), estado, { estado = it }, Modifier.weight(1f))
            }
            if (isLote) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditField(stringResource(R.string.profile_edit_street), calle, { calle = it }, Modifier.weight(2f))
                    EditField(stringResource(R.string.profile_edit_number), numero, { numero = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditField(stringResource(R.string.profile_edit_colony), colonia, { colonia = it }, Modifier.weight(2f))
                    EditField(
                        stringResource(R.string.profile_edit_zip), codigoPostal, { codigoPostal = it }, Modifier.weight(1f),
                        keyboardType = KeyboardType.Number
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            EditField(stringResource(R.string.profile_edit_bio), bio, { bio = it }, singleLine = false)

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onChangePassword,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, KarsyBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = KarsyWhite, contentColor = KarsyNavy),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.profile_change_password), fontFamily = DmSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            (if (missing) stringResource(R.string.profile_edit_missing) else error)?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, fontFamily = DmSans, fontSize = 13.sp, color = KarsyError)
            }

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
                    Text(stringResource(R.string.profile_cancel), fontFamily = DmSans, fontSize = 14.sp)
                }
                Button(
                    onClick = {
                        val name = if (isLote) firstName.trim()
                        else listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                        // La BD exige nombre y ubicación (y la dirección completa de un lote).
                        val required = listOf(name, municipio, estado) +
                            if (isLote) listOf(calle, numero, colonia, codigoPostal) else emptyList()
                        missing = required.any { it.isBlank() }
                        phonesError = validatePhones(phone.trim(), whatsapp.trim(), medio)
                        if (!saving && !missing && phonesError == null) onSave(
                            user.copy(
                                displayName = name,
                                phone = phone.trim(),
                                whatsapp = whatsapp.trim(),
                                medioPrincipal = medio,
                                bio = bio.trim(),
                                municipio = municipio.trim(),
                                estado = estado.trim(),
                                location = "${municipio.trim()}, ${estado.trim()}",
                                responsable = responsable.trim(),
                                calle = calle.trim(),
                                numero = numero.trim(),
                                colonia = colonia.trim(),
                                codigoPostal = codigoPostal.trim(),
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
                        stringResource(if (saving) R.string.profile_edit_saving else R.string.profile_edit_save),
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
    enabled: Boolean = true,
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
            enabled = enabled,
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

/** Mismas reglas que el registro: al menos un número, de 10 dígitos o más, y el principal con número. */
private fun validatePhones(phone: String, whatsapp: String, medio: String): String? {
    fun valido(n: String) = n.count { it.isDigit() } >= 10
    return when {
        phone.isEmpty() && whatsapp.isEmpty() -> texto(R.string.profile_phones_empty)
        (phone.isNotEmpty() && !valido(phone)) || (whatsapp.isNotEmpty() && !valido(whatsapp)) ->
            texto(R.string.auth_error_phone_digits)
        medio == UserRepository.LLAMADA && phone.isEmpty() -> texto(R.string.profile_phones_main_needs_phone)
        medio == UserRepository.WHATSAPP && whatsapp.isEmpty() -> texto(R.string.profile_phones_main_needs_whatsapp)
        else -> null
    }
}

/** Elige el medio de contacto principal: llamada o WhatsApp (solo uno). */
@Composable
private fun MainContactPicker(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(
            stringResource(R.string.profile_phones_main_title),
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                UserRepository.LLAMADA to stringResource(R.string.profile_phones_calls),
                UserRepository.WHATSAPP to stringResource(R.string.profile_phones_whatsapp),
            ).forEach { (value, label) ->
                val active = selected == value
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) KarsyTeal.copy(alpha = 0.08f) else KarsyBg)
                        .border(1.dp, if (active) KarsyTeal else KarsyBorder, RoundedCornerShape(10.dp))
                        .clickable { onSelect(value) }
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    RadioButton(
                        selected = active,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = KarsyTeal),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        label,
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) KarsyTeal else KarsyCharcoal,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
