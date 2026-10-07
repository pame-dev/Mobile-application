package com.pame.karsy.core.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Muestra un teléfono de 10 dígitos como "871 123 4567" mientras se escribe.
 * Solo es visual: el valor guardado sigue siendo "8711234567". Con lada ("+52…") no se agrupa.
 */
object PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.startsWith("+")) return TransformedText(text, OffsetMapping.Identity)
        val out = buildString {
            raw.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append(' ')
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            // Posición en el texto original → posición en el texto con espacios.
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 6 -> offset + 1
                else -> offset + 2
            }.coerceAtMost(out.length)

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 7 -> offset - 1
                else -> offset - 2
            }.coerceIn(0, raw.length)
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}
