package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.Outfit

private val OtpBorder = Color(0xFFDDE5E9)

/**
 * Casillas del código de verificación (una por dígito); avanzan/retroceden el foco solas.
 * Si se pega el código completo en cualquier casilla se reparte entre todas.
 */
@Composable
fun OtpInput(otp: MutableList<String>, modifier: Modifier = Modifier, autoFocus: Boolean = false) {
    val focusers = remember { List(otp.size) { FocusRequester() } }
    if (autoFocus) LaunchedEffect(Unit) { focusers.first().requestFocus() }

    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (otp.size > 4) 8.dp else 12.dp, Alignment.CenterHorizontally)
    ) {
        otp.forEachIndexed { index, digit ->
            val shape = RoundedCornerShape(14.dp)
            BasicTextField(
                value = digit,
                onValueChange = { raw ->
                    val typed = raw.filter { it.isDigit() }
                    if (typed.length <= 2) {
                        otp[index] = typed.takeLast(1)
                        if (otp[index].isNotEmpty() && index < otp.lastIndex) focusers[index + 1].requestFocus()
                    } else {
                        val start = if (typed.length >= otp.size) 0 else index
                        val pasted = typed.takeLast(otp.size - start)
                        pasted.forEachIndexed { i, c -> otp[start + i] = c.toString() }
                        focusers[minOf(start + pasted.length, otp.lastIndex)].requestFocus()
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(
                    fontFamily = Outfit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(KarsyTeal),
                modifier = Modifier
                    // Hasta 56dp por casilla; con 6 dígitos se encogen para caber en pantallas angostas.
                    .weight(1f, fill = false)
                    .widthIn(max = 56.dp)
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(shape)
                    .background(KarsyBg)
                    .border(if (digit.isNotEmpty()) 1.8.dp else 1.5.dp, if (digit.isNotEmpty()) KarsyTeal else OtpBorder, shape)
                    .focusRequester(focusers[index])
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace && otp[index].isEmpty() && index > 0) {
                            focusers[index - 1].requestFocus()
                            true
                        } else false
                    },
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
                }
            )
        }
    }
}
