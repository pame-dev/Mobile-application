package com.pame.karsy.core.components

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
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
) {
    var internal by rememberSaveable { mutableStateOf("") }
    val text = value ?: internal
    var show by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Text(
            text = label,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = text,
            onValueChange = { onValueChange?.invoke(it) ?: run { internal = it } },
            placeholder = {
                Text(placeholder, fontFamily = DmSans, fontSize = 15.sp, color = KarsyMid)
            },
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 15.sp, color = KarsyCharcoal),
            visualTransformation = if (isPassword && !show) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType
            ),
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { show = !show }) {
                        Icon(
                            if (show) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (show) "Ocultar contraseña" else "Mostrar contraseña",
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
        )
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
fun karsyTextFieldColors(container: androidx.compose.ui.graphics.Color = KarsyWhite) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = KarsyTeal,
        unfocusedBorderColor = KarsyBorder,
        errorBorderColor = KarsyError,
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        errorContainerColor = container,
        cursorColor = KarsyTeal,
    )
