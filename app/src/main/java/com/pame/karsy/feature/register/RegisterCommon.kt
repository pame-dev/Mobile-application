package com.pame.karsy.feature.register

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.ScrollState
import com.pame.karsy.core.theme.KarsyWarning
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.pame.karsy.core.components.PhoneVisualTransformation
import com.pame.karsy.core.theme.KarsyTeal
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.util.FormRules
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
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
            color = KarsyInk
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
internal fun ContactFields(vm: RegisterViewModel, phoneLabel: String, onPositioned: (String, Float) -> Unit = { _, _ -> }) {
    FormInput(
        label = phoneLabel, placeholder = stringResource(R.string.auth_phone_placeholder), keyboardType = KeyboardType.Phone,
        value = vm.telefono, onValueChange = { vm.telefono = it }, inputFilter = FormRules::telefonoAlEscribir, error = vm.errorFor("telefono"), onFocusLost = { vm.touch("telefono") },
        // Se ve "871 123 4567"; se guarda "8711234567".
        visualTransformation = PhoneVisualTransformation,
        onPositioned = { onPositioned("telefono", it) }
    )
    FieldNotice(vm.telefonoEnUsoAviso)

    FieldQuestion(stringResource(R.string.auth_same_whatsapp_question))
    OptionButtons(
        options = listOf(stringResource(R.string.auth_yes), stringResource(R.string.auth_no)),
        selected = if (vm.mismoWhatsapp) 0 else 1,
        onSelect = { vm.mismoWhatsapp = it == 0 },
        modifier = Modifier.padding(bottom = 16.dp)
    )
    FormInput(
        label = "WhatsApp",
        placeholder = if (vm.mismoWhatsapp) stringResource(R.string.auth_whatsapp_same_as_phone) else stringResource(R.string.auth_whatsapp_other_optional),
        keyboardType = KeyboardType.Phone,
        value = if (vm.mismoWhatsapp) vm.telefono else vm.whatsapp,
        onValueChange = { vm.whatsapp = it },
        inputFilter = FormRules::telefonoAlEscribir,
        enabled = !vm.mismoWhatsapp,
        error = vm.errorFor("whatsapp"), onFocusLost = { vm.touch("whatsapp") },
        visualTransformation = PhoneVisualTransformation,
        onPositioned = { onPositioned("whatsapp", it) }
    )

    Box(Modifier.onGloballyPositioned { onPositioned("medio", it.positionInRoot().y) }) {
        FieldQuestion(stringResource(R.string.auth_contact_method_question))
    }
    OptionButtons(
        options = listOf("WhatsApp", stringResource(R.string.auth_contact_calls)),
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

/** "¿Quisiste decir pame@gmail.com?": un toque reemplaza el correo. No bloquea el registro. */
@Composable
internal fun EmailSuggestion(vm: RegisterViewModel) {
    val sugerido = vm.correoSugerido ?: return
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.form_email_suggestion_prefix))
            withStyle(SpanStyle(color = KarsyTeal, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                append(sugerido)
            }
            append("?")
        },
        fontFamily = DmSans,
        fontSize = 13.sp,
        color = KarsyCharcoal,
        modifier = Modifier
            .offset(y = (-8).dp)
            .padding(bottom = 8.dp, start = 4.dp)
            .clickable { vm.correo = sugerido }
    )
}

/**
 * Aviso debajo de un campo. Sin [actionLabel] es un aviso en ámbar que no bloquea
 * (CP de otro estado, teléfono en otra cuenta); con acción se muestra como error con
 * un botón (p. ej. "Iniciar sesión" si el correo ya tiene cuenta).
 */
@Composable
internal fun FieldNotice(message: String?, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    if (message == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(y = (-8).dp)
            .padding(start = 4.dp, bottom = 8.dp)
    ) {
        Text(
            message,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = if (actionLabel != null) KarsyError else KarsyWarning,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyTeal,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable(onClick = onAction)
            )
        }
    }
}

/**
 * Lleva la vista al primer campo con error al tocar "Crear perfil". Cada campo informa
 * su posición en pantalla con [mark]; [viewport] va en el contenedor que hace scroll.
 */
internal class ErrorScroller(val scrollState: ScrollState) {
    private val positions = mutableStateMapOf<String, Float>()
    private var viewportTop = 0f

    fun mark(field: String): (Float) -> Unit = { positions[field] = it }

    fun markModifier(vararg fields: String): Modifier = Modifier.onGloballyPositioned { c ->
        val y = c.positionInRoot().y
        fields.forEach { positions[it] = y }
    }

    val viewport: Modifier = Modifier.onGloballyPositioned { viewportTop = it.positionInRoot().y }

    suspend fun scrollTo(fields: List<String>, marginPx: Float) {
        val y = fields.mapNotNull { positions[it] }.minOrNull() ?: return
        scrollState.animateScrollTo((scrollState.value + y - viewportTop - marginPx).toInt().coerceAtLeast(0))
    }
}

@Composable
internal fun rememberErrorScroller(vm: RegisterViewModel, esLote: Boolean): ErrorScroller {
    val scroller = remember { ErrorScroller(ScrollState(0)) }
    val margin = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(vm.scrollToErrorRequest) {
        if (vm.scrollToErrorRequest > 0) scroller.scrollTo(vm.camposConError(esLote), margin)
    }
    return scroller
}

/** "Iniciar sesión" debajo del correo cuando ya tiene cuenta. */
@Composable
internal fun EmailExistsAction(vm: RegisterViewModel, onLogin: () -> Unit) {
    if (!vm.correoYaRegistrado) return
    Text(
        stringResource(R.string.form_action_login),
        fontFamily = DmSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = KarsyTeal,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .offset(y = (-8).dp)
            .padding(start = 4.dp, bottom = 8.dp)
            .clickable(onClick = onLogin)
    )
}
