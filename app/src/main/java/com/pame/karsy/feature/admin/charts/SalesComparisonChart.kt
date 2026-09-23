package com.pame.karsy.feature.admin.charts

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal

/**
 * Gráfica de barras agrupadas "Comparación de ventas": ventas vía Karsy vs. fuera de la
 * plataforma. El último mes resalta la barra "fuera" en azul marino, como en el mockup.
 */
@Composable
fun SalesComparisonChart(
    labels: List<String>,
    viaPlataforma: List<Int>,
    fuera: List<Int>,
    modifier: Modifier = Modifier,
    gridValues: List<Int> = listOf(0, 50, 100, 150, 200),
) {
    val textMeasurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontFamily = DmSans, fontSize = 10.sp, color = KarsyMid)
    val valueStyle = TextStyle(
        fontFamily = DmSans,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = KarsyNavy
    )
    val maxValue = ((viaPlataforma + fuera).maxOrNull() ?: 1) * 1.15f

    Canvas(modifier) {
        val left = 34.dp.toPx()
        val right = size.width
        val top = 16.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val plotHeight = bottom - top

        fun yOf(v: Float) = bottom - (v / maxValue) * plotHeight

        gridValues.forEach { v ->
            val y = yOf(v.toFloat())
            if (y < 0f) return@forEach
            drawLine(KarsyBg, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
            val layout = textMeasurer.measure(v.toString(), axisStyle)
            drawText(
                layout,
                topLeft = Offset(left - 8.dp.toPx() - layout.size.width, y - layout.size.height / 2f)
            )
        }

        val groupWidth = (right - left) / labels.size
        val barWidth = minOf(26.dp.toPx(), groupWidth * 0.3f)
        val gap = minOf(6.dp.toPx(), groupWidth * 0.07f)
        val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())

        labels.forEachIndexed { i, label ->
            val center = left + i * groupWidth + groupWidth / 2f
            val isLast = i == labels.lastIndex

            val vPlat = viaPlataforma.getOrElse(i) { 0 }
            val vFuera = fuera.getOrElse(i) { 0 }
            val yPlat = yOf(vPlat.toFloat())
            val yFuera = yOf(vFuera.toFloat())

            val xPlat = center - barWidth - gap / 2f
            drawRoundRect(
                color = KarsyTeal.copy(alpha = 0.95f),
                topLeft = Offset(xPlat, yPlat),
                size = Size(barWidth, bottom - yPlat),
                cornerRadius = radius
            )
            val xFuera = center + gap / 2f
            drawRoundRect(
                color = if (isLast) KarsyNavy else KarsyBorderMuted.copy(alpha = 0.9f),
                topLeft = Offset(xFuera, yFuera),
                size = Size(barWidth, bottom - yFuera),
                cornerRadius = radius
            )

            val platLayout = textMeasurer.measure(vPlat.toString(), valueStyle)
            drawText(
                platLayout,
                topLeft = Offset(
                    xPlat + barWidth / 2f - platLayout.size.width / 2f,
                    yPlat - 3.dp.toPx() - platLayout.size.height
                )
            )
            val fueraLayout = textMeasurer.measure(vFuera.toString(), valueStyle)
            drawText(
                fueraLayout,
                topLeft = Offset(
                    xFuera + barWidth / 2f - fueraLayout.size.width / 2f,
                    yFuera - 3.dp.toPx() - fueraLayout.size.height
                )
            )

            val labelLayout = textMeasurer.measure(label, axisStyle)
            drawText(
                labelLayout,
                topLeft = Offset(center - labelLayout.size.width / 2f, bottom + 6.dp.toPx())
            )
        }
    }
}
