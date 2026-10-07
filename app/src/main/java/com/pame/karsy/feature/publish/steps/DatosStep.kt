package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyErrorLight
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.feature.publish.FieldLabel
import com.pame.karsy.feature.publish.PublishStepScaffold
import com.pame.karsy.feature.publish.PublishTextInput
import com.pame.karsy.feature.publish.PublishViewModel
import com.pame.karsy.feature.publish.SectionCard
import com.pame.karsy.feature.publish.SectionTitle

// Colores para las muestras; los nombres vienen del catálogo public.colores.
private val coloresHex = mapOf(
    "Blanco" to Color(0xFFFFFFFF),
    "Negro" to Color(0xFF1A1A1A),
    "Gris" to Color(0xFF6B7280),
    "Plata" to Color(0xFFB0B8C1),
    "Rojo" to Color(0xFFDC2626),
    "Azul" to Color(0xFF2563EB),
    "Verde" to Color(0xFF16A34A),
    "Beige" to Color(0xFFC9A87C),
    "Café" to Color(0xFF7C4A21),
    "Amarillo" to Color(0xFFFACC15),
    "Naranja" to Color(0xFFEA580C),
    "Vino" to Color(0xFF7F1D1D),
)

/** Paso 1: datos generales del vehículo. Marcas, modelos y demás salen de la BD. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatosStep(form: PublishViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    val catalogs = form.catalogs
    // "Otros" de color, transmisión y carrocería no se ofrecen (requerirían texto libre).
    val transmisiones = catalogs?.transmisiones.orEmpty().filterNot { it.esOtro }
    val carrocerias = catalogs?.carrocerias.orEmpty().filterNot { it.esOtro }
    val colores = catalogs?.colores.orEmpty().filterNot { it.esOtro }

    PublishStepScaffold(
        step = 1,
        form = form,
        onBack = onBack,
        buttonText = stringResource(R.string.publish_next_photos),
        onButtonClick = onNext
    ) {
        form.error?.let { StepError(it) }
        form.rejectedReason?.let { RejectedNotice(it) }

        SectionCard {
            SectionTitle(stringResource(R.string.publish_section_vehicle_info))

            Column {
                FieldLabel(stringResource(R.string.publish_label_brand))
                CatalogDropdown(
                    value = catalogs?.marcas?.firstOrNull { it.id == form.idMarca }?.nombre.orEmpty(),
                    placeholder = if (catalogs == null) stringResource(R.string.publish_loading_brands) else stringResource(R.string.publish_select_brand),
                    options = catalogs?.marcas.orEmpty().map { it.id to it.nombre },
                    onSelect = form::selectMarca
                )
                if (form.marcaEsOtro) {
                    PublishTextInput(form.marcaOtra, { form.marcaOtra = it.take(60) }, stringResource(R.string.publish_enter_brand))
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_model))
                CatalogDropdown(
                    value = catalogs?.modelos?.firstOrNull { it.id == form.idModelo }?.nombre.orEmpty(),
                    placeholder = if (form.idMarca == null) stringResource(R.string.publish_choose_brand_first) else stringResource(R.string.publish_select_model),
                    options = catalogs?.modelosDe(form.idMarca).orEmpty().map { it.id to it.nombre },
                    onSelect = { form.idModelo = it }
                )
                if (form.modeloEsOtro) {
                    PublishTextInput(form.modeloOtro, { form.modeloOtro = it.take(60) }, stringResource(R.string.publish_enter_model))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_year))
                    PublishTextInput(form.anio, { form.anio = it }, "2023", numeric = true, maxDigits = 4)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_price))
                    PublishTextInput(form.precio, { form.precio = it }, "685000", numeric = true, maxDigits = 10)
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_transmission))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transmisiones.forEach { t ->
                        val selected = form.idTransmision == t.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) KarsyNavy else KarsyBg)
                                .border(1.5.dp, if (selected) KarsyNavy else KarsyBorderMuted, RoundedCornerShape(12.dp))
                                .clickable { form.idTransmision = t.id }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t.nombre,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) KarsyWhite else KarsyMid
                            )
                        }
                    }
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_mileage))
                PublishTextInput(form.kilometraje, { form.kilometraje = it }, "18500", numeric = true, maxDigits = 7)
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_insurance_recovered))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        true to stringResource(R.string.publish_yes),
                        false to stringResource(R.string.publish_no),
                    ).forEach { (valor, etiqueta) ->
                        val selected = form.recuperadoPorSeguro == valor
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) KarsyNavy else KarsyBg)
                                .border(1.5.dp, if (selected) KarsyNavy else KarsyBorderMuted, RoundedCornerShape(12.dp))
                                .clickable { form.recuperadoPorSeguro = valor }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                etiqueta,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) KarsyWhite else KarsyMid
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_cylinders))
                    PublishTextInput(form.cilindros, { form.cilindros = it }, "4", numeric = true, maxDigits = 2)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_horsepower))
                    PublishTextInput(form.caballos, { form.caballos = it }, "184", numeric = true, maxDigits = 4)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_engine))
                    PublishTextInput(form.motor, { form.motor = it.take(PublishViewModel.MOTOR_MAX) }, "2.0 L Turbo")
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_fuel))
                    val combustibles = PublishViewModel.COMBUSTIBLES
                    CatalogDropdown(
                        value = form.combustible,
                        placeholder = stringResource(R.string.publish_select_fuel),
                        options = combustibles.mapIndexed { i, c -> i.toLong() to c },
                        onSelect = { form.combustible = combustibles[it.toInt()] }
                    )
                }
            }
        }

        SectionCard {
            SectionTitle(stringResource(R.string.publish_section_classification))

            Column {
                FieldLabel(stringResource(R.string.publish_label_body_type))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    carrocerias.forEach { t ->
                        val selected = form.idCarroceria == t.id
                        Text(
                            text = t.nombre,
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) KarsyWhite else KarsyMid,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) KarsyNavy else KarsyBg)
                                .border(1.5.dp, if (selected) KarsyNavy else KarsyBorderMuted, RoundedCornerShape(50))
                                .clickable { form.idCarroceria = t.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_color))
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colores.forEach { c ->
                        ColorSwatch(
                            name = c.nombre,
                            color = coloresHex[c.nombre] ?: KarsyBorderMuted,
                            selected = form.idColor == c.id,
                            onClick = { form.idColor = c.id }
                        )
                    }
                }
            }
        }

        SectionCard {
            SectionTitle(stringResource(R.string.publish_section_additional_info))
            Column {
                FieldLabel(stringResource(R.string.publish_label_previous_owners))
                PublishTextInput(form.duenos, { form.duenos = it }, "1", numeric = true, maxDigits = 2)
            }
        }
    }
}

/** Mensaje de validación arriba del formulario. */
/** Motivo del rechazo, arriba del formulario al corregir una publicación. */
@Composable
private fun RejectedNotice(reason: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KarsyErrorLight)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            stringResource(R.string.publish_edit_rejected_title),
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyError
        )
        Text(
            reason,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = KarsyCharcoal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
internal fun StepError(message: String) {
    Text(
        message,
        fontFamily = DmSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = KarsyError,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KarsyErrorLight)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@Composable
private fun CatalogDropdown(
    value: String,
    placeholder: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KarsyBg)
                .border(1.5.dp, KarsyBorderMuted, RoundedCornerShape(12.dp))
                .clickable(enabled = options.isNotEmpty()) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                fontFamily = DmSans,
                fontSize = 14.sp,
                color = if (value.isNotEmpty()) KarsyCharcoal else KarsyMid,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = KarsyMid,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = KarsySurface,
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            options.forEach { (id, nombre) ->
                DropdownMenuItem(
                    text = {
                        Text(nombre, fontFamily = DmSans, fontSize = 14.sp, color = KarsyCharcoal)
                    },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(if (selected) Modifier.border(2.dp, KarsyTeal, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        if (selected) 3.dp else 2.dp,
                        if (selected) KarsyNavy else KarsyBorderMuted,
                        CircleShape
                    )
            )
        }
        Text(
            text = name,
            fontFamily = DmSans,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) KarsyInk else KarsyMid,
            textAlign = TextAlign.Center
        )
    }
}
