package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.Formato
import com.pame.karsy.feature.publish.PublishStepScaffold
import com.pame.karsy.feature.cardetail.ImageViewer
import com.pame.karsy.feature.publish.PublishViewModel
import kotlinx.coroutines.launch

/** Paso 4: vista previa de la publicación (datos y fotos de los pasos anteriores) y "Publicar". */
@Composable
fun ConfirmStep(form: PublishViewModel, onPublish: () -> Unit, onBack: () -> Unit) {
    val carImages = form.fotosElegidas
    val pager = rememberPagerState { carImages.size }
    val imgIndex = pager.currentPage
    val scope = rememberCoroutineScope()
    val goTo = { page: Int -> scope.launch { pager.animateScrollToPage(page) }; Unit }
    var showViewer by rememberSaveable { mutableStateOf(false) }
    val techSpecs = listOf(
        stringResource(R.string.publish_spec_model) to form.modeloNombre,
        stringResource(R.string.publish_spec_year) to form.anio.trim(),
        stringResource(R.string.publish_spec_brand) to form.marcaNombre,
        stringResource(R.string.publish_spec_transmission) to form.transmisionNombre,
        stringResource(R.string.publish_spec_mileage) to form.kilometraje.filter(Char::isDigit).toIntOrNull().let { Formato.km(it) },
        stringResource(R.string.publish_spec_cylinders) to form.cilindros.ifBlank { "—" },
        stringResource(R.string.publish_spec_horsepower) to form.caballos.filter(Char::isDigit).let { if (it.isEmpty()) "—" else "$it hp" },
        stringResource(R.string.publish_spec_engine) to form.motor.trim().ifEmpty { "—" },
        stringResource(R.string.publish_spec_fuel) to form.combustible.ifEmpty { "—" },
        stringResource(R.string.publish_spec_body_type) to form.carroceriaNombre,
        stringResource(R.string.publish_spec_color) to form.colorNombre,
        stringResource(R.string.publish_spec_previous_owners) to form.duenos.ifBlank { "0" },
    )

    Box(Modifier.fillMaxSize()) {
        PublishStepScaffold(
            step = 4,
            form = form,
            onBack = onBack,
            buttonText = stringResource(
                when {
                    form.isEditing && form.publishing -> R.string.publish_resubmitting
                    form.editingApproved -> R.string.publish_send_changes
                    form.isEditing -> R.string.publish_resubmit
                    form.publishing -> R.string.publish_publishing
                    else -> R.string.publish_publish
                }
            ),
            onButtonClick = onPublish,
            contentPadding = PaddingValues(0.dp),
            spacing = 0
        ) {
            form.error?.let {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) { StepError(it) }
            }
            if (form.publishing) {
                LinearProgressIndicator(
                    color = KarsyTeal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }
            // Carrusel principal
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KarsyCharcoal)
            ) {
                // Se desliza entre fotos; al tocar se abre a pantalla completa.
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    AsyncImage(
                        model = carImages[page],
                        contentDescription = form.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showViewer = true }
                    )
                }
                if (carImages.size > 1) CarouselArrow(
                    icon = Icons.Rounded.ChevronLeft,
                    description = stringResource(R.string.publish_previous),
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                    onClick = { goTo((imgIndex - 1 + carImages.size) % carImages.size) }
                )
                if (carImages.size > 1) CarouselArrow(
                    icon = Icons.Rounded.ChevronRight,
                    description = stringResource(R.string.publish_next),
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                    onClick = { goTo((imgIndex + 1) % carImages.size) }
                )
                if (carImages.isNotEmpty()) Text(
                    "${imgIndex + 1} / ${carImages.size}",
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyWhite,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(KarsyNavy.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            // Miniaturas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                carImages.forEachIndexed { i, src ->
                    val selected = imgIndex == i
                    AsyncImage(
                        model = src,
                        contentDescription = stringResource(R.string.publish_thumbnail, i + 1),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 64.dp, height = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, if (selected) KarsyNavy else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { goTo(i) }
                    )
                }
            }

            // Modelo y precio
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text(
                    "${form.marcaNombre} ${form.modeloNombre} · ${form.anio.trim()}",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    color = KarsyMid,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    buildAnnotatedString {
                        append(form.precioTexto + " ")
                        withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)) { append("MXN") }
                    },
                    fontFamily = Outfit,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyTeal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Ficha técnica
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                BlockLabel(stringResource(R.string.publish_block_specs))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KarsyWhite)
                        .border(1.dp, KarsyBorderMuted, RoundedCornerShape(16.dp))
                ) {
                    techSpecs.forEachIndexed { i, (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                key,
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                color = KarsyMid,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                value,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KarsyCharcoal
                            )
                        }
                        if (i < techSpecs.lastIndex) HorizontalDivider(thickness = 1.dp, color = KarsyBg)
                    }
                }
            }

            // Descripción e imperfecciones
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                BlockLabel(stringResource(R.string.publish_block_description))
                TextBlock(form.descripcion.trim(), background = KarsyWhite, border = KarsyBorderMuted)
                Box(Modifier.padding(top = 16.dp)) { BlockLabel(stringResource(R.string.publish_block_imperfections)) }
                TextBlock(
                    form.imperfecciones.trim().ifEmpty { stringResource(R.string.publish_no_imperfections) },
                    background = Color(0xFFFFFBF0),
                    border = Color(0xFFF5E8B0)
                )
                Text(
                    stringResource(R.string.publish_review_notice),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    color = KarsyMid,
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
            Box(Modifier.height(24.dp))
        }

        if (showViewer && carImages.isNotEmpty()) {
            ImageViewer(
                images = carImages.map { it.toString() },
                startIndex = imgIndex,
                contentDescription = form.titulo,
                onClose = { last ->
                    showViewer = false
                    scope.launch { pager.scrollToPage(last) }
                }
            )
        }
    }
}

@Composable
private fun CarouselArrow(
    icon: ImageVector,
    description: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(KarsyWhite.copy(alpha = 0.88f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = KarsyNavy, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BlockLabel(text: String) {
    Text(
        text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05.em,
        color = KarsyNavy,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun TextBlock(text: String, background: Color, border: Color) {
    Text(
        text,
        fontFamily = DmSans,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        color = KarsyCharcoal,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    )
}
