package com.pame.karsy.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite

/**
 * Campo de formulario con etiqueta arriba (FormInput del mockup).
 * Si [isPassword] es true muestra el ícono de ojo para ver/ocultar.
 * Si no se pasa [value]/[onValueChange] el campo guarda su propio estado (útil mientras
 * no hay lógica real detrás).
 */
@Composable
fun FormInput(
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
    enabled: Boolean = true,
    /** Se llama al salir del campo (para validarlo en ese momento). */
    onFocusLost: (() -> Unit)? = null,
    /** Filtro al escribir (p. ej. FormRules.soloDigitos): lo que no pasa no se escribe. */
    inputFilter: ((String) -> String)? = null,
    /** Mayúscula automática del teclado (nombres: Words). */
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    /** Formato solo visual (p. ej. PhoneVisualTransformation); no cambia el valor. */
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Si se indica, muestra el contador "120/300" debajo del campo. */
    maxChars: Int? = null,
    /** Posición vertical del campo en pantalla (para llevar la vista al primer error). */
    onPositioned: ((Float) -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var internal by rememberSaveable { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val text = value ?: internal
    var show by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .padding(bottom = 16.dp)
            .then(if (onPositioned != null) Modifier.onGloballyPositioned { onPositioned(it.positionInRoot().y) } else Modifier)
    ) {
        Text(
            text = label,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        val (fieldValue, onFieldChange) = rememberFilteredFieldValue(text, inputFilter) {
            onValueChange?.invoke(it) ?: run { internal = it }
        }
        OutlinedTextField(
            value = fieldValue,
            onValueChange = onFieldChange,
            placeholder = {
                Text(placeholder, fontFamily = DmSans, fontSize = 15.sp, color = KarsyMid)
            },
            singleLine = singleLine,
            minLines = minLines,
            enabled = enabled,
            isError = error != null,
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 15.sp, color = KarsyCharcoal),
            visualTransformation = if (isPassword && !show) PasswordVisualTransformation() else visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                capitalization = capitalization,
                // "Siguiente" en el teclado pasa al próximo campo.
                imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { show = !show }) {
                        Icon(
                            if (show) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = stringResource(if (show) R.string.core_hide_password else R.string.core_show_password),
                            tint = KarsyMid
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(12.dp),
            colors = karsyTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .onFocusChanged { state ->
                    if (hadFocus && !state.isFocused) onFocusLost?.invoke()
                    hadFocus = state.isFocused
                }
        )
        if (maxChars != null) {
            Text(
                "${text.length}/$maxChars",
                fontFamily = DmSans,
                fontSize = 11.sp,
                color = if (text.length >= maxChars) KarsyError else KarsyMid,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp, end = 4.dp)
            )
        }
        if (error != null) {
            Text(
                text = error,
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyError,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun karsyTextFieldColors(container: androidx.compose.ui.graphics.Color = KarsySurface) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KarsyTeal,
        unfocusedBorderColor = KarsyBorder,
        errorBorderColor = KarsyError,
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        errorContainerColor = container,
        // Campo bloqueado (p. ej. WhatsApp igual al teléfono): se lee, pero con fondo gris.
        disabledContainerColor = KarsyBg,
        disabledBorderColor = KarsyBorder,
        disabledTextColor = KarsyCharcoal,
        cursorColor = KarsyTeal,
    )
