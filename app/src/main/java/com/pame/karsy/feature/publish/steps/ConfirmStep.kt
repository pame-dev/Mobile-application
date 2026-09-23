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
import com.pame.karsy.feature.publish.PublishStepScaffold

// TODO: mostrar la vista previa con los datos y fotos capturados en los pasos anteriores (PublishViewModel)
private val techSpecs = listOf(
    "Modelo" to "Serie 3 320i",
    "Año" to "2023",
    "Marca" to "BMW",
    "Transmisión" to "Automática",
    "Kilometraje" to "18,500 km",
    "Cilindros" to "4 en línea",
    "Caballos de fuerza" to "184 hp",
    "Tipo de carro" to "Sedán",
    "Color" to "Blanco Alpino",
    "Cant. de dueños" to "1",
)

private val carImages = listOf(
    "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800&h=500&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1550355291-bbee04a92027?w=800&h=500&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1542282088-fe8426682b8f?w=200&h=150&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1550355291-bbee04a92027?w=200&h=150&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=200&h=150&fit=crop&auto=format",
)

private const val DESCRIPTION =
    "BMW Serie 3 320i en excelente estado, primer dueño, factura original. Motor 2.0L turbo de 184 hp, " +
        "transmisión automática de 8 velocidades. Cuenta con techo corredizo, Apple CarPlay, asientos de cuero " +
        "y sistema de frenado automático."

private const val IMPERFECTIONS =
    "Pequeño rayón en parachoque trasero lado derecho (pintado). Resto del vehículo sin golpes ni abolladuras. " +
        "Tapicería impecable, sin olores."

/** Paso 4: vista previa de la publicación y botón "Publicar". */
@Composable
fun ConfirmStep(onPublish: () -> Unit, onBack: () -> Unit) {
    var imgIndex by rememberSaveable { mutableIntStateOf(0) }

    PublishStepScaffold(
        step = 4,
        onBack = onBack,
        buttonText = "Publicar",
        onButtonClick = onPublish,
        contentPadding = PaddingValues(0.dp),
        spacing = 0
    ) {
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
                model = carImages[imgIndex],
                contentDescription = "BMW Serie 3",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            CarouselArrow(
                icon = Icons.Rounded.ChevronLeft,
                description = "Anterior",
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                onClick = { imgIndex = (imgIndex - 1 + carImages.size) % carImages.size }
            )
            CarouselArrow(
                icon = Icons.Rounded.ChevronRight,
                description = "Siguiente",
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                onClick = { imgIndex = (imgIndex + 1) % carImages.size }
            )
            Text(
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
                "Serie 3 320i · 2023",
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = KarsyMid,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                buildAnnotatedString {
                    append("$685,000 ")
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
            TextBlock(DESCRIPTION, background = KarsyWhite, border = KarsyBorderMuted)
            Box(Modifier.padding(top = 16.dp)) { BlockLabel("DETALLADO O IMPERFECCIONES") }
            TextBlock(IMPERFECTIONS, background = Color(0xFFFFFBF0), border = Color(0xFFF5E8B0))
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
