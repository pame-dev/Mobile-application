package com.pame.karsy.feature.publish

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/*
 * Componentes compartidos del flujo "Publicar vehículo" (Header, Stepper, FieldLabel,
 * TextInput, SectionCard y barra inferior del mockup). Paleta PUBLISH_C -> colores Karsy.
 */

/** Encabezado con botón circular atrás y título centrado. */
@Composable
fun PublishHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(KarsyBg)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = "Regresar",
                tint = KarsyNavy,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = title,
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = KarsyNavy,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 36.dp)
        )
    }
}

private val stepLabels = listOf("Datos", "Fotos", "Detalles", "Confirmar")

/** Indicador de progreso de 4 pasos. [step] va de 1 a 4. */
@Composable
fun PublishStepper(step: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        stepLabels.forEachIndexed { i, label ->
            val num = i + 1
            val done = num < step
            val active = num == step
            val inactive = num > step
            val stepItem = @Composable {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (done || active) KarsyNavy else Color.Transparent)
                            .border(2.dp, if (inactive) KarsyMid else KarsyNavy, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = KarsyWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = num.toString(),
                                fontFamily = DmSans,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) KarsyWhite else KarsyMid
                            )
                        }
                    }
                    Text(
                        text = label,
                        fontFamily = DmSans,
                        fontSize = 10.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (inactive) KarsyMid else KarsyNavy,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            if (i < stepLabels.lastIndex) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    stepItem()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 3.dp, end = 3.dp, top = 13.dp)
                            .height(2.dp)
                            .alpha(if (inactive) 0.4f else 1f)
                            .background(if (num < step) KarsyNavy else KarsyMid)
                    )
                }
            } else {
                stepItem()
            }
        }
    }
}

/** Etiqueta en mayúsculas encima de cada campo. */
@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05.em,
        color = KarsyMid,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

/** Título de cada tarjeta de sección ("INFORMACIÓN DEL VEHÍCULO"...). */
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = KarsyNavy
    )
}

/** Campo de texto del formulario de publicación. */
@Composable
fun PublishTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    background: Color = KarsyWhite,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = KarsyCharcoal),
        cursorBrush = SolidColor(KarsyNavy),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
        ),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .border(1.5.dp, KarsyBorderMuted, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, fontFamily = DmSans, fontSize = 14.sp, color = KarsyMid)
                }
                inner()
            }
        }
    )
}

/** Tarjeta blanca con sombra suave que agrupa campos. */
@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = KarsyNavy.copy(alpha = 0.2f), spotColor = KarsyNavy.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(KarsyWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

/** Botón redondeado azul marino de la barra inferior. */
@Composable
fun PublishPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
        contentPadding = PaddingValues(vertical = 15.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text, fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

/**
 * Estructura común de cada paso: encabezado, stepper, contenido desplazable y
 * barra inferior fija con el botón para continuar.
 */
@Composable
fun PublishStepScaffold(
    step: Int,
    onBack: () -> Unit,
    buttonText: String,
    onButtonClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    spacing: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .imePadding()
    ) {
        PublishHeader(title = "Publicar vehículo", onBack = onBack)
        PublishStepper(step = step)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            content()
            Spacer(Modifier.height(16.dp))
        }
        HorizontalDivider(thickness = 1.dp, color = KarsyBorderMuted)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KarsyBg)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)
        ) {
            PublishPrimaryButton(text = buttonText, onClick = onButtonClick)
        }
    }
}
