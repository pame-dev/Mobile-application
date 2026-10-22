package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.locale.rememberTraduccion
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.util.Constancia
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
import java.util.Calendar

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

        // Cada campo se activa hasta que los anteriores están llenos (formulario en orden).
        val activo = { campo: String -> !form.bloqueado(campo) }
        val noAplica = stringResource(R.string.publish_not_applicable)

        Text(
            stringResource(R.string.publish_fill_in_order),
            fontFamily = DmSans,
            fontSize = 12.sp,
            color = KarsyMid
        )

        SectionCard {
            SectionTitle(stringResource(R.string.publish_section_vehicle_info))

            Column {
                FieldLabel(stringResource(R.string.publish_label_brand))
                CatalogDropdown(
                    value = catalogs?.marcas?.firstOrNull { it.id == form.idMarca }?.nombre.orEmpty(),
                    placeholder = if (catalogs == null) stringResource(R.string.publish_loading_brands) else stringResource(R.string.publish_select_brand),
                    options = catalogs?.marcas.orEmpty().map { it.id to it.nombre },
                    onSelect = form::selectMarca,
                    selectedId = form.idMarca
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
                    onSelect = { form.idModelo = it },
                    enabled = activo("modelo"),
                    selectedId = form.idModelo
                )
                if (form.modeloEsOtro) {
                    PublishTextInput(form.modeloOtro, { form.modeloOtro = it.take(60) }, stringResource(R.string.publish_enter_model), enabled = activo("modelo"))
                }
            }

            // El precio va al final del formulario (paso Detalles), con la sugerencia de precio.
            Column {
                FieldLabel(stringResource(R.string.publish_label_year))
                // Del año que viene hacia atrás, hasta ANIO_MIN.
                val anioMax = Calendar.getInstance().get(Calendar.YEAR) + 1
                CatalogDropdown(
                    value = form.anio,
                    placeholder = stringResource(R.string.publish_select_year),
                    options = (anioMax downTo PublishViewModel.ANIO_MIN).map { it.toLong() to it.toString() },
                    onSelect = { form.anio = it.toString() },
                    enabled = activo("anio"),
                    selectedId = form.anio.toLongOrNull()
                )
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_transmission))
                FlowRow(
                    modifier = Modifier.alpha(if (activo("transmision")) 1f else 0.5f),
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
                                .clickable(enabled = activo("transmision")) { form.idTransmision = t.id }
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
                PublishTextInput(form.kilometraje, { form.kilometraje = it }, "18500", numeric = true, maxDigits = 7, enabled = activo("kilometraje"))
            }
        }

        SectionCard {
            SectionTitle(stringResource(R.string.publish_section_classification))

            Column {
                FieldLabel(stringResource(R.string.publish_label_body_type))
                FlowRow(
                    modifier = Modifier.alpha(if (activo("carroceria")) 1f else 0.5f),
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
                                .clickable(enabled = activo("carroceria")) { form.idCarroceria = t.id }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_color))
                FlowRow(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .alpha(if (activo("color")) 1f else 0.5f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colores.forEach { c ->
                        ColorSwatch(
                            name = c.nombre,
                            color = coloresHex[c.nombre] ?: KarsyBorderMuted,
                            selected = form.idColor == c.id,
                            onClick = { form.idColor = c.id },
                            enabled = activo("color")
                        )
                    }
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_previous_owners))
                val max = PublishViewModel.DUENOS_MAX
                val masDe = stringResource(R.string.publish_owners_or_more, max)
                CatalogDropdown(
                    value = form.duenos.toIntOrNull()?.let { if (it >= max) masDe else it.toString() }.orEmpty(),
                    placeholder = stringResource(R.string.publish_select),
                    options = (0..max).map { it.toLong() to if (it == max) masDe else it.toString() },
                    onSelect = { form.duenos = it.toString() },
                    enabled = activo("duenos")
                )
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_condition))
                OpcionesSeleccion(
                    opciones = PublishViewModel.CONDICIONES.map { (valor, etiqueta) -> valor to stringResource(etiqueta) },
                    seleccionado = form.condicion,
                    enabled = activo("condicion"),
                    onSelect = { form.condicion = it }
                )
            }
        }

        SectionCard {
            SectionTitle(stringResource(R.string.publish_section_additional_info))

            Column {
                FieldLabel(stringResource(R.string.publish_label_fuel))
                val combustibles = PublishViewModel.COMBUSTIBLES
                CatalogDropdown(
                    value = form.combustible,
                    placeholder = stringResource(R.string.publish_select),
                    options = combustibles.mapIndexed { i, c -> i.toLong() to c },
                    onSelect = { form.selectCombustible(combustibles[it.toInt()]) },
                    enabled = activo("combustible")
                )
            }

            // Un eléctrico no lleva cilindros, litros ni aspiración.
            val electrico = form.esElectrico
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_cylinders))
                    CatalogDropdown(
                        value = form.cilindros,
                        placeholder = if (electrico) noAplica else stringResource(R.string.publish_select),
                        options = PublishViewModel.CILINDROS.map { it.toLong() to it.toString() },
                        onSelect = { form.cilindros = it.toString() },
                        enabled = !electrico && activo("cilindros")
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_engine_liters))
                    PublishTextInput(
                        form.litros,
                        { form.litros = PublishViewModel.litrosAlEscribir(it) },
                        if (electrico) noAplica else "2.0",
                        numeric = true,
                        enabled = !electrico && activo("litros")
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_aspiration))
                    val aspiraciones = PublishViewModel.ASPIRACIONES.map { (valor, etiqueta) -> valor to stringResource(etiqueta) }
                    CatalogDropdown(
                        value = aspiraciones.firstOrNull { it.first == form.aspiracion }?.second.orEmpty(),
                        placeholder = if (electrico) noAplica else stringResource(R.string.publish_select),
                        options = aspiraciones.mapIndexed { i, (_, etiqueta) -> i.toLong() to etiqueta },
                        onSelect = { form.aspiracion = aspiraciones[it.toInt()].first },
                        enabled = !electrico && activo("aspiracion")
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(stringResource(R.string.publish_label_horsepower))
                    PublishTextInput(form.caballos, { form.caballos = it }, "184", numeric = true, maxDigits = 4, enabled = activo("caballos"))
                }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_insurance_recovered))
                Row(
                    modifier = Modifier.alpha(if (activo("recuperado")) 1f else 0.5f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                .clickable(enabled = activo("recuperado")) { form.recuperadoPorSeguro = valor }
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

            val siNo = listOf(true to stringResource(R.string.publish_yes), false to stringResource(R.string.publish_no))
            Column {
                FieldLabel(stringResource(R.string.publish_label_original_invoice))
                OpcionesSeleccion(siNo, form.facturaOriginal, activo("factura")) { form.facturaOriginal = it }
            }

            Column {
                FieldLabel(stringResource(R.string.publish_label_air_conditioning))
                OpcionesSeleccion(siNo, form.aireAcondicionado, activo("aire")) { form.aireAcondicionado = it }
            }
        }

        DocumentoSection(form, enabled = activo("documento"))
    }
}

/** Botones de opción única (Sí / No, Seminuevo / Usado), con el mismo estilo que "Recuperado por el seguro". */
@Composable
private fun <T> OpcionesSeleccion(opciones: List<Pair<T, String>>, seleccionado: T, enabled: Boolean, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { (valor, etiqueta) ->
            val selected = seleccionado == valor
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) KarsyNavy else KarsyBg)
                    .border(1.5.dp, if (selected) KarsyNavy else KarsyBorderMuted, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { onSelect(valor) }
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

/**
 * Documento del vehículo: se elige uno; el VIN y la placa se escriben, la tarjeta de
 * circulación y la baja van con foto (JPG o PNG) o PDF (bucket privado, sin tapar placas).
 */
@Composable
private fun DocumentoSection(form: PublishViewModel, enabled: Boolean) {
    val context = LocalContext.current
    // Selector de archivos: ofrece fotos y PDF; el tipo real lo revisa el ViewModel.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { form.elegirArchivoDocumento(context, it) }
    }
    val tipos = listOf(
        "vin" to R.string.publish_doc_vin,
        "placa" to R.string.publish_doc_plate,
        "tarjeta_circulacion" to R.string.publish_doc_card,
        "baja" to R.string.publish_doc_baja,
    ).map { (valor, etiqueta) -> valor to stringResource(etiqueta) }

    SectionCard {
        SectionTitle(stringResource(R.string.publish_section_document))
        Text(
            stringResource(R.string.publish_document_hint),
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = KarsyMid
        )

        Column {
            FieldLabel(stringResource(R.string.publish_label_document_type))
            CatalogDropdown(
                value = tipos.firstOrNull { it.first == form.tipoDocumento }?.second.orEmpty(),
                placeholder = stringResource(R.string.publish_select),
                options = tipos.mapIndexed { i, (_, etiqueta) -> i.toLong() to etiqueta },
                onSelect = { form.selectTipoDocumento(tipos[it.toInt()].first) },
                enabled = enabled
            )
        }

        when {
            form.tipoDocumento == "vin" || form.tipoDocumento == "placa" -> Column {
                val esVin = form.tipoDocumento == "vin"
                FieldLabel(stringResource(if (esVin) R.string.publish_label_vin else R.string.publish_label_plate))
                PublishTextInput(
                    form.documentoTexto,
                    form::escribirDocumento,
                    if (esVin) "3VWFE21C04M000001" else "ABC-123-D",
                    enabled = enabled
                )
            }
            form.documentoEsFoto -> Column {
                FieldLabel(stringResource(R.string.publish_label_document_photo))
                val shape = RoundedCornerShape(12.dp)
                // Foto con vista previa; PDF con su nombre. Sin archivo: invita a elegir uno.
                val imagen = form.documentoArchivo.takeIf { !form.documentoEsPdf }
                val conArchivo = imagen != null || form.documentoEsPdf
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(shape)
                        .background(KarsyBg)
                        .border(1.5.dp, if (form.documentoError != null) KarsyError else KarsyBorderMuted, shape)
                        .alpha(if (enabled) 1f else 0.5f)
                        .clickable(enabled = enabled) { picker.launch(Constancia.TIPOS_SELECTOR) },
                    contentAlignment = Alignment.Center
                ) {
                    if (imagen != null) {
                        AsyncImage(
                            model = imagen,
                            contentDescription = stringResource(R.string.publish_label_document_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (form.documentoEsPdf) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 40.dp)
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = KarsyNavy, modifier = Modifier.size(36.dp))
                            Text(
                                form.documentoNombre,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KarsyCharcoal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .then(if (conArchivo) Modifier.align(Alignment.BottomCenter).padding(10.dp) else Modifier)
                            .clip(RoundedCornerShape(50))
                            .background(if (conArchivo) KarsyNavy else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = null,
                            tint = if (conArchivo) KarsyWhite else KarsyMid,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            stringResource(if (conArchivo) R.string.publish_change_document_photo else R.string.publish_add_document_photo),
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (conArchivo) KarsyWhite else KarsyMid
                        )
                    }
                }
                Text(
                    form.documentoError ?: stringResource(R.string.publish_document_file_hint, Constancia.MAX_MB),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    color = if (form.documentoError != null) KarsyError else KarsyMid,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
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
            rememberTraduccion(reason),
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

/**
 * Lista desplegable de catálogo (Marca, Modelo, Año). El menú queda anclado justo debajo del campo
 * y de su mismo ancho (ExposedDropdownMenuBox), con scroll interno. Al abrirlo, el campo se desplaza
 * a la vista con espacio para la lista, así no se abre encima del campo ni lo saca de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogDropdown(
    value: String,
    placeholder: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit,
    /** false: se ve atenuado y no se abre (formulario en orden). */
    enabled: Boolean = true,
    /** Opción elegida, que se marca en la lista. */
    selectedId: Long? = null,
) {
    val activo = enabled && options.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }
    val enVista = remember { BringIntoViewRequester() }
    var alto by remember { mutableStateOf(0) }
    val altoMenu = with(LocalDensity.current) { MenuMaxHeight.toPx() }
    // Al abrir, trae a la vista el campo y el espacio de la lista debajo de él.
    LaunchedEffect(expanded) {
        if (expanded) enVista.bringIntoView(Rect(0f, 0f, 1f, alto + altoMenu))
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (activo) expanded = it }) {
        Row(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = activo)
                .bringIntoViewRequester(enVista)
                .onSizeChanged { alto = it.height }
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KarsyBg)
                .border(1.5.dp, KarsyBorderMuted, RoundedCornerShape(12.dp))
                .alpha(if (enabled) 1f else 0.5f)
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = KarsySurface,
            modifier = Modifier.heightIn(max = MenuMaxHeight)
        ) {
            options.forEach { (id, nombre) ->
                val elegido = id == selectedId
                DropdownMenuItem(
                    text = {
                        Text(
                            nombre,
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                            fontWeight = if (elegido) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (elegido) KarsyTeal else KarsyCharcoal
                        )
                    },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                    modifier = if (elegido) Modifier.background(KarsyBg) else Modifier
                )
            }
        }
    }
}

/** Alto máximo de las listas desplegables (con scroll interno). */
private val MenuMaxHeight = 300.dp

@Composable
private fun ColorSwatch(name: String, color: Color, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
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



