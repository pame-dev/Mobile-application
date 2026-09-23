package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.feature.publish.PublishStepScaffold

private data class PhotoSlot(val label: String, val imageUrl: String?)

// Datos de ejemplo del mockup: sólo la foto frontal aparece cargada.
private val photoSlots = listOf(
    PhotoSlot("Foto frontal", "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=120&h=90&fit=crop&auto=format"),
    PhotoSlot("Foto trasera", null),
    PhotoSlot("Foto lateral izquierda", null),
    PhotoSlot("Foto lateral derecha", null),
    PhotoSlot("Foto del tablero", null),
)

/** Paso 2: fotos del vehículo. */
@Composable
fun PhotosStep(onNext: () -> Unit, onBack: () -> Unit) {
    PublishStepScaffold(
        step = 2,
        onBack = onBack,
        buttonText = "Siguiente: Detalles →",
        onButtonClick = onNext,
        spacing = 12
    ) {
        photoSlots.forEachIndexed { i, slot ->
            PhotoSlotCard(slot = slot, isDashboard = i == 4)
        }
    }
}

@Composable
private fun PhotoSlotCard(slot: PhotoSlot, isDashboard: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KarsyWhite)
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawRoundRect(
                    color = KarsyMid,
                    topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KarsyBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isDashboard) Icons.Rounded.Speed else Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    tint = KarsyTeal,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                slot.label,
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyCharcoal,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KarsyBg),
                contentAlignment = Alignment.Center
            ) {
                if (slot.imageUrl != null) {
                    AsyncImage(
                        model = slot.imageUrl,
                        contentDescription = slot.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(KarsyTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = KarsyWhite, modifier = Modifier.size(12.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .drawBehind {
                                drawCircle(
                                    color = KarsyMid,
                                    radius = size.minDimension / 2 - 0.75.dp.toPx(),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = KarsyMid, modifier = Modifier.scale(0.7f))
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KarsyWhite)
                .border(1.dp, KarsyBorderMuted, RoundedCornerShape(12.dp))
                .clickable {
                    // TODO: abrir selector de imágenes (galería) y guardar la foto de este ángulo
                }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Image, null, tint = KarsyMid, modifier = Modifier.size(18.dp))
            Text(
                "Elegir de galería",
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = KarsyCharcoal,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
