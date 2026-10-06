package com.pame.karsy.core.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCardOutline
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.Tema

/**
 * Esqueletos de carga: bloques grises con un brillo que los recorre, con la forma del
 * contenido que va a aparecer. Se usan en lugar del círculo girando mientras carga una lista.
 */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1_300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress"
    )
    val base = KarsyBorder
    val highlight = if (Tema.oscuro) base.copy(alpha = 0.55f) else KarsySurface
    this
        .clip(shape)
        .background(base)
        .background(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(progress * 1000f - 500f, 0f),
                end = Offset(progress * 1000f, 300f)
            )
        )
}

/** Bloque de esqueleto de [width]×[height] (o todo el ancho si [width] es null). */
@Composable
fun SkeletonBlock(height: Dp, width: Dp? = null, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(
        (if (width != null) modifier.width(width) else modifier.fillMaxWidth())
            .height(height)
            .shimmer(shape)
    )
}

/** Tarjeta de vehículo en carga (misma forma que VehicleCard / FeaturedCarCard). */
@Composable
fun SkeletonCarCard(modifier: Modifier = Modifier, imageHeight: Dp = 180.dp) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KarsySurface)
            .border(1.dp, KarsyCardOutline, shape)
    ) {
        SkeletonBlock(imageHeight, shape = RoundedCornerShape(0.dp))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(16.dp, width = 140.dp)
                Spacer(Modifier.weight(1f))
                SkeletonBlock(16.dp, width = 80.dp)
            }
            SkeletonBlock(12.dp, width = 90.dp)
            SkeletonBlock(12.dp)
            SkeletonBlock(12.dp, width = 200.dp)
        }
    }
}

/** Fila de lista en carga: círculo o miniatura + dos líneas de texto. */
@Composable
fun SkeletonListRow(modifier: Modifier = Modifier, circle: Boolean = false) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).shimmer(if (circle) CircleShape else RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBlock(13.dp, width = 170.dp)
            SkeletonBlock(11.dp)
        }
    }
}
