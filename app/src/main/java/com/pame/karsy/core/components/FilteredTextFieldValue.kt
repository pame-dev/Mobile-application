package com.pame.karsy.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Valor de un campo de texto con filtro al escribir (solo dígitos, solo letras…).
 *
 * Si se filtra sobre un String, el teclado conserva su propia copia del texto (con la
 * letra rechazada) y al seguir escribiendo se desincroniza y se come caracteres. Aquí,
 * cuando el filtro cambia el texto, se manda un valor nuevo sin composición y con el
 * cursor al final: el teclado se resincroniza.
 *
 * Devuelve el valor a mostrar y el onValueChange para el campo.
 */
@Composable
fun rememberFilteredFieldValue(
    text: String,
    filter: ((String) -> String)?,
    onTextChange: (String) -> Unit,
): Pair<TextFieldValue, (TextFieldValue) -> Unit> {
    var field by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    // Si el texto cambió desde fuera (p. ej. al cargar datos), se toma ese.
    val shown = if (field.text == text) field else TextFieldValue(text, TextRange(text.length))
    val onChange: (TextFieldValue) -> Unit = { nuevo ->
        val filtrado = filter?.invoke(nuevo.text) ?: nuevo.text
        field = if (filtrado == nuevo.text) nuevo else TextFieldValue(filtrado, TextRange(filtrado.length))
        onTextChange(filtrado)
    }
    return shown to onChange
}
