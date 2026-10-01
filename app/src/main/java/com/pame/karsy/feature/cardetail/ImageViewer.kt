package com.pame.karsy.feature.cardetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * Visor de fotos a pantalla completa: fondo negro, deslizar entre fotos,
 * pellizco para zoom y doble tap para acercar/alejar.
 * Empieza en [startIndex] y al cerrar devuelve la foto que se estaba viendo.
 */
@Composable
fun ImageViewer(
    images: List<String>,
    startIndex: Int,
    contentDescription: String,
    onClose: (lastIndex: Int) -> Unit,
) {
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))) { images.size }
    val close = { onClose(pager.currentPage) }
    BackHandler(onBack = close)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Bloquea los toques para que no lleguen a la pantalla de detalle de abajo.
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            ZoomableImage(
                model = images[page],
                contentDescription = contentDescription,
                // Al cambiar de foto, la que quedó atrás vuelve a su tamaño normal.
                isCurrent = pager.settledPage == page,
            )
        }

        // Barra superior transparente: cerrar + contador.
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = close,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.home_close), tint = Color.White, modifier = Modifier.size(24.dp))
            }
            if (images.isNotEmpty()) Text(
                stringResource(R.string.detail_photo_counter, pager.currentPage + 1, images.size),
                color = Color.White,
                fontFamily = DmSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/** Limita el desplazamiento para que la foto ampliada no se salga de la pantalla. */
private fun clampOffset(o: Offset, scale: Float, size: IntSize): Offset {
    val maxX = size.width * (scale - 1f) / 2f
    val maxY = size.height * (scale - 1f) / 2f
    return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
}

/**
 * Imagen con pellizco para zoom, arrastre y doble tap. Sin zoom, el deslizamiento
 * horizontal pasa al carrusel; con zoom se arrastra la foto, y al llegar al borde
 * el gesto vuelve a pasar al carrusel para cambiar de foto.
 */
@Composable
private fun ZoomableImage(model: String, contentDescription: String, isCurrent: Boolean) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val fromScale = scale
                        val fromOffset = offset
                        val toScale = if (scale > 1.05f) 1f else DOUBLE_TAP_ZOOM
                        // Acerca hacia el punto tocado; al alejar regresa al centro.
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val toOffset = if (toScale == 1f) Offset.Zero
                        else clampOffset((center - tap) * (toScale - 1f), toScale, size)
                        scope.launch {
                            animate(0f, 1f, animationSpec = tween(250)) { t, _ ->
                                scale = fromScale + (toScale - fromScale) * t
                                offset = fromOffset + (toOffset - fromOffset) * t
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.count { it.pressed } > 1
                        if (pinching || scale > 1f) {
                            val pan = event.calculatePan()
                            val newScale = (scale * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                            val wanted = offset + pan
                            val newOffset = clampOffset(wanted, newScale, size)
                            scale = newScale
                            offset = newOffset
                            // Arrastre horizontal que ya topó con el borde: se lo dejamos al carrusel.
                            val pushingPastEdge = !pinching && newOffset.x != wanted.x && abs(pan.x) > abs(pan.y)
                            if (!pushingPastEdge) event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
