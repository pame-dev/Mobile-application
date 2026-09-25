package com.pame.karsy.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
/** Título grande + subtítulo gris que usan las pantallas de registro. */
@Composable
internal fun RegisterHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp)
    ) {
        Text(
            title,
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).em,
            color = KarsyNavy
        )
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontFamily = DmSans, fontSize = 15.sp, lineHeight = 21.sp, color = KarsyMid)
    }
}

/** Mensaje de error debajo del botón "Crear perfil". */
@Composable
internal fun RegisterError(message: String?) {
    if (message == null) return
    Text(
        message,
        fontFamily = DmSans,
        fontSize = 13.sp,
        color = KarsyError,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}

/**
 * Teléfono, WhatsApp y método de contacto principal.
 * Si usa el mismo número para WhatsApp, ese campo muestra el teléfono y queda bloqueado.
 */
@Composable
internal fun ContactFields(vm: RegisterViewModel, phoneLabel: String) {
    FormInput(
        label = phoneLabel, placeholder = "10 dígitos", keyboardType = KeyboardType.Phone,
        value = vm.telefono, onValueChange = { vm.telefono = it }, error = vm.errorFor("telefono")
    )

    FieldQuestion("¿Usas este mismo número para WhatsApp?")
    OptionButtons(
        options = listOf("Sí", "No"),
        selected = if (vm.mismoWhatsapp) 0 else 1,
        onSelect = { vm.mismoWhatsapp = it == 0 },
        modifier = Modifier.padding(bottom = 16.dp)
    )
    FormInput(
        label = "WhatsApp",
        placeholder = if (vm.mismoWhatsapp) "Igual a tu teléfono" else "Otro número (opcional)",
        keyboardType = KeyboardType.Phone,
        value = if (vm.mismoWhatsapp) vm.telefono else vm.whatsapp,
        onValueChange = { vm.whatsapp = it },
        enabled = !vm.mismoWhatsapp,
        error = vm.errorFor("whatsapp")
    )

    FieldQuestion("Método de contacto principal")
    OptionButtons(
        options = listOf("WhatsApp", "Llamadas"),
        selected = when (vm.medioContacto) {
            RegisterViewModel.WHATSAPP -> 0
            RegisterViewModel.LLAMADA -> 1
            else -> -1
        },
        onSelect = { vm.medioContacto = if (it == 0) RegisterViewModel.WHATSAPP else RegisterViewModel.LLAMADA }
    )
    vm.errorFor("medio")?.let {
        Text(it, fontFamily = DmSans, fontSize = 12.sp, color = KarsyError, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
    }
    Spacer(Modifier.height(16.dp))
}

/** Pregunta con el mismo estilo que la etiqueta de FormInput. */
@Composable
private fun FieldQuestion(text: String) {
    Text(
        text,
        fontFamily = DmSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = KarsyCharcoal,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/** Botones de opción del mismo ancho (estilo de "Transmisión" al publicar). -1 = ninguno elegido. */
@Composable
private fun OptionButtons(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, option ->
            val active = i == selected
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) KarsyNavy else KarsyBg)
                    .border(1.5.dp, if (active) KarsyNavy else KarsyBorderMuted, shape)
                    .clickable { onSelect(i) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) KarsyWhite else KarsyMid
                )
            }
        }
    }
}
