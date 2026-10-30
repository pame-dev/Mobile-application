package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.PrecioSugerido
import com.pame.karsy.feature.publish.FieldLabel
import com.pame.karsy.feature.publish.LocalCampoConError
import com.pame.karsy.feature.publish.PublishField
import com.pame.karsy.feature.publish.PublishStepScaffold
import com.pame.karsy.feature.publish.PublishTextInput
import com.pame.karsy.feature.publish.PublishViewModel

/** Paso 3: descripción, imperfecciones y, al final, el precio con la sugerencia de autos similares. */
@Composable
fun DetailsStep(form: PublishViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    PublishStepScaffold(
        step = 3,
        form = form,
        onBack = onBack,
        buttonText = stringResource(R.string.publish_next_confirmation),
        onButtonClick = onNext
    ) {
        form.error?.let { StepError(it) }
        val tipBold = stringResource(R.string.publish_tip_bold, 60)
        val tipFull = stringResource(R.string.publish_tip_full, tipBold)
        PublishField(form, "descripcion", marcarAlSalir = true) {
            LimitedTextArea(
                title = stringResource(R.string.publish_description_title),
                value = form.descripcion,
                onValueChange = { form.descripcion = it },
                placeholder = stringResource(R.string.publish_description_placeholder),
                maxChars = 500
            )
        }
        LimitedTextArea(
            title = stringResource(R.string.publish_imperfections_title),
            value = form.imperfecciones,
            onValueChange = { form.imperfecciones = it },
            placeholder = stringResource(R.string.publish_imperfections_placeholder),
            maxChars = 300
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KarsyRadius.Lg))
                .background(KarsyTealSoft)
                .border(1.dp, KarsyTeal.copy(alpha = 0.13f), RoundedCornerShape(KarsyRadius.Lg))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(KarsyTeal),
                contentAlignment = Alignment.Center
            ) {
                Text("i", fontFamily = DmSans, fontSize = KarsyTextSize.Body, fontWeight = FontWeight.Bold, color = KarsyWhite)
            }
            Text(
                text = buildAnnotatedString {
                    append(tipFull)
                    val boldStart = tipFull.indexOf(tipBold)
                    if (boldStart >= 0) {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), boldStart, boldStart + tipBold.length)
                    }
                },
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                lineHeight = 19.sp,
                color = KarsyInk
            )
        }

        // El precio va al final del formulario, con la sugerencia de autos similares.
        LaunchedEffect(form.idModelo, form.anio, form.modeloEsOtro) { form.cargarSugerenciaPrecio() }
        PublishField(form, "precio", marcarAlSalir = true) {
            FieldLabel(stringResource(R.string.publish_label_price))
            PublishTextInput(form.precio, { form.precio = it }, "685000", numeric = true, maxDigits = 10)
        }
        SugerenciaPrecioCard(form)
    }
}

/** Rango de precios de autos similares y cómo queda el precio escrito frente a él. */
@Composable
private fun SugerenciaPrecioCard(form: PublishViewModel) {
    val s = form.sugerenciaPrecio
    if (form.buscandoPrecio) {
        Text(stringResource(R.string.publish_price_loading), fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyMid)
        return
    }
    if (s == null) {
        if (form.idModelo != null && !form.modeloEsOtro && !form.errorPrecio) {
            Text(stringResource(R.string.publish_price_no_data), fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyMid)
        }
        return
    }
    val modelo = listOf(form.marcaNombre, form.modeloNombre).filter { it.isNotEmpty() }.joinToString(" ")
    val grupo = if (s.desde != null && s.hasta != null) stringResource(R.string.publish_price_group_years, modelo, s.desde, s.hasta)
    else stringResource(R.string.publish_price_group_all_years, modelo)
    val precio = form.precio.filter(Char::isDigit).toLongOrNull() ?: 0
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KarsyRadius.Lg))
            .background(KarsyTealSoft)
            .border(1.dp, KarsyTeal.copy(alpha = 0.13f), RoundedCornerShape(KarsyRadius.Lg))
            .padding(16.dp)
    ) {
        Text(
            stringResource(
                R.string.publish_price_suggest,
                grupo,
                s.rango.n,
                Formato.precio(s.rango.min.toDouble()),
                Formato.precio(s.rango.max.toDouble()),
                Formato.precio(s.rango.tipico.toDouble()),
            ),
            fontFamily = DmSans,
            fontSize = KarsyTextSize.Body,
            lineHeight = 19.sp,
            color = KarsyInk
        )
        if (precio > 0) {
            Text(
                stringResource(
                    when (PrecioSugerido.comparar(precio, s.rango)) {
                        PrecioSugerido.Comparacion.ABAJO -> R.string.publish_price_below
                        PrecioSugerido.Comparacion.DENTRO -> R.string.publish_price_within
                        PrecioSugerido.Comparacion.ARRIBA -> R.string.publish_price_above
                    }
                ),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Body,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
        }
        if (precio != s.rango.tipico) {
            Text(
                stringResource(R.string.publish_price_use_typical, Formato.precio(s.rango.tipico.toDouble())),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Body,
                fontWeight = FontWeight.SemiBold,
                color = KarsyInk,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = form::usarPrecioTipico)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun LimitedTextArea(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxChars: Int,
) {
    val conError = LocalCampoConError.current
    Column {
        Text(
            title,
            fontFamily = DmSans,
            fontSize = KarsyTextSize.Body,
            fontWeight = FontWeight.Bold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KarsyRadius.Lg))
                .background(KarsySurface)
                .border(if (conError) 1.5.dp else 1.dp, if (conError) KarsyError else KarsyBorderMuted, RoundedCornerShape(KarsyRadius.Lg))
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(maxChars)) },
                minLines = 5,
                textStyle = TextStyle(fontFamily = DmSans, fontSize = KarsyTextSize.Body, lineHeight = 19.sp, color = KarsyCharcoal),
                cursorBrush = SolidColor(KarsyInk),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                fontFamily = DmSans,
                                fontSize = KarsyTextSize.Body,
                                lineHeight = 19.sp,
                                color = KarsyCharcoal.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                }
            )
            Text(
                "${value.length} / $maxChars",
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Caption,
                color = KarsyMid,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    }
}
