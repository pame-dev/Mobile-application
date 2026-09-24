package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
import com.pame.karsy.feature.publish.PublishViewModel

/** Paso 4: vista previa de la publicación (datos y fotos de los pasos anteriores) y "Publicar". */
@Composable
fun ConfirmStep(form: PublishViewModel, onPublish: () -> Unit, onBack: () -> Unit) {
    var imgIndex by rememberSaveable { mutableIntStateOf(0) }
    val carImages = form.fotosElegidas
    val techSpecs = listOf(
        "Modelo" to form.modeloNombre,
        "Año" to form.anio.trim(),
        "Marca" to form.marcaNombre,
        "Transmisión" to form.transmisionNombre,
        "Kilometraje" to form.kilometraje.filter(Char::isDigit).toIntOrNull().let { Formato.km(it) },
        "Cilindros" to form.cilindros.ifBlank { "—" },
        "Caballos de fuerza" to form.caballos.filter(Char::isDigit).let { if (it.isEmpty()) "—" else "$it hp" },
        "Tipo de carro" to form.carroceriaNombre,
        "Color" to form.colorNombre,
        "Dueños anteriores" to form.duenos.ifBlank { "0" },
    )

    PublishStepScaffold(
        step = 4,
        onBack = onBack,
        buttonText = if (form.publishing) "Publicando…" else "Publicar",
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
            AsyncImage(
                model = carImages.getOrNull(imgIndex),
                contentDescription = form.titulo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (carImages.size > 1) CarouselArrow(
                icon = Icons.Rounded.ChevronLeft,
                description = "Anterior",
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                onClick = { imgIndex = (imgIndex - 1 + carImages.size) % carImages.size }
            )
            if (carImages.size > 1) CarouselArrow(
                icon = Icons.Rounded.ChevronRight,
                description = "Siguiente",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                onClick = { imgIndex = (imgIndex + 1) % carImages.size }
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
            carImages.drop(1).forEachIndexed { i, src ->
                val selected = imgIndex == i + 1
                AsyncImage(
                    model = src,
                    contentDescription = "Miniatura ${i + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 64.dp, height = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, if (selected) KarsyNavy else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { imgIndex = i + 1 }
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
            BlockLabel("FICHA TÉCNICA")
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
            BlockLabel("DESCRIPCIÓN")
            TextBlock(form.descripcion.trim(), background = KarsyWhite, border = KarsyBorderMuted)
            Box(Modifier.padding(top = 16.dp)) { BlockLabel("DETALLADO O IMPERFECCIONES") }
            TextBlock(
                form.imperfecciones.trim().ifEmpty { "Sin imperfecciones reportadas." },
                background = Color(0xFFFFFBF0),
                border = Color(0xFFF5E8B0)
            )
            Text(
                "Tu publicación se revisará antes de aparecer en el inicio.",
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
        Box(Modifier.height(24.dp))
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
